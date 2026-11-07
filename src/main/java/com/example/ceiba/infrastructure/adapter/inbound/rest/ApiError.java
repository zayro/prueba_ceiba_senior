package com.example.ceiba.infrastructure.adapter.inbound.rest;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String error, String message) {
}
