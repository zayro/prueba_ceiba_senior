package com.example.ceiba.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

public record ChangeStateRequest(@NotBlank String state) {
}
