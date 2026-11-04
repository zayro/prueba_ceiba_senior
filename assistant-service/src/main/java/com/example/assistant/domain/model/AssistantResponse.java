package com.example.assistant.domain.model;

import java.util.List;

public record AssistantResponse(String respuesta, List<ProtocolSource> fuentes) {
}
