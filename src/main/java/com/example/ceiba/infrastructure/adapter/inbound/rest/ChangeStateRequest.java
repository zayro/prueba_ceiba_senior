package com.example.ceiba.infrastructure.adapter.inbound.rest;

import jakarta.validation.constraints.NotBlank;

public record ChangeStateRequest(@NotBlank String state) {
}
