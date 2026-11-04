package com.example.assistant.infrastructure.adapter.inbound.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsultRequest(@NotBlank @Size(max = 2000) String pregunta) {
}
