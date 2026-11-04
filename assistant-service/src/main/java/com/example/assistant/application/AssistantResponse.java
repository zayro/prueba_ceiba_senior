package com.example.assistant.application;

import java.util.List;

public record AssistantResponse(String respuesta, List<ProtocolSource> fuentes) {
}
