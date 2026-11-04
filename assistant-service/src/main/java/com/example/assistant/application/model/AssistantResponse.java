package com.example.assistant.application.model;

import java.util.List;

public record AssistantResponse(String respuesta, List<ProtocolSource> fuentes) {
}
