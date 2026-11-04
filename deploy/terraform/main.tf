terraform {
  required_version = ">= 1.5.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

provider "azurerm" {
  subscription_id = var.subscription_id

  features {}
}

provider "random" {}

variable "subscription_id" {
  description = "Azure subscription ID. If omitted, ARM_SUBSCRIPTION_ID must be set."
  type        = string
  default     = null
}

variable "location" {
  description = "Azure region for the resource group, AKS, and ACR."
  type        = string
  default     = "eastus2"
}

variable "resource_group_name" {
  description = "Name of the resource group to create."
  type        = string
  default     = "rg-vidacruz-dev"
}

variable "name_prefix" {
  description = "Lowercase alphanumeric prefix used for globally unique Azure resource names."
  type        = string
  default     = "vidacruz"

  validation {
    condition     = can(regex("^[a-z0-9]{3,10}$", var.name_prefix))
    error_message = "name_prefix must contain 3 to 10 lowercase letters or digits."
  }
}

variable "node_vm_size" {
  description = "VM size for the AKS system node pool."
  type        = string
  default     = "Standard_D2s_v5"
}

variable "node_count_min" {
  description = "Minimum number of AKS system nodes."
  type        = number
  default     = 2
}

variable "node_count_max" {
  description = "Maximum number of AKS system nodes."
  type        = number
  default     = 3
}

resource "random_string" "suffix" {
  length  = 5
  lower   = true
  upper   = false
  numeric = true
  special = false
}

locals {
  registry_name = "${var.name_prefix}${random_string.suffix.result}"
  cluster_name  = "aks-${var.name_prefix}-${random_string.suffix.result}"
}

resource "azurerm_resource_group" "main" {
  name     = var.resource_group_name
  location = var.location
}

resource "azurerm_container_registry" "main" {
  name                = local.registry_name
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  sku                 = "Basic"
  admin_enabled       = false
}

resource "azurerm_kubernetes_cluster" "main" {
  name                      = local.cluster_name
  location                  = azurerm_resource_group.main.location
  resource_group_name       = azurerm_resource_group.main.name
  dns_prefix                = local.cluster_name
  sku_tier                  = "Free"
  automatic_upgrade_channel = "patch"

  identity {
    type = "SystemAssigned"
  }

  default_node_pool {
    name                 = "system"
    vm_size              = var.node_vm_size
    auto_scaling_enabled = true
    min_count            = var.node_count_min
    max_count            = var.node_count_max
    node_count           = var.node_count_min
    os_sku               = "Ubuntu"
  }

  network_profile {
    network_plugin      = "azure"
    network_plugin_mode = "overlay"
  }

  oidc_issuer_enabled       = true
  workload_identity_enabled = true
}

resource "azurerm_role_assignment" "aks_acr_pull" {
  scope                = azurerm_container_registry.main.id
  role_definition_name = "AcrPull"
  principal_id         = azurerm_kubernetes_cluster.main.kubelet_identity[0].object_id
}

output "resource_group_name" {
  description = "Resource group containing AKS and ACR."
  value       = azurerm_resource_group.main.name
}

output "aks_cluster_name" {
  description = "AKS cluster name."
  value       = azurerm_kubernetes_cluster.main.name
}

output "acr_name" {
  description = "Azure Container Registry name."
  value       = azurerm_container_registry.main.name
}

output "acr_login_server" {
  description = "Registry hostname for tagging and pushing application images."
  value       = azurerm_container_registry.main.login_server
}

output "kubeconfig_command" {
  description = "Command to configure kubectl for the new AKS cluster."
  value       = "az aks get-credentials --resource-group ${azurerm_resource_group.main.name} --name ${azurerm_kubernetes_cluster.main.name}"
}
