package com.example.ceiba.infrastructure.adapter.inbound.rest;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RegisterPatientRequest(
	@NotBlank @Size(min = 3, max = 100) String fullName,
	@NotNull @Min(0) @Max(120) Integer age,
	@NotNull @Min(20) @Max(250) Integer heartRate,
	@NotNull @Min(60) @Max(260) Integer systolicPressure,
	@NotNull @Min(30) @Max(160) Integer diastolicPressure,
	@NotNull @Min(70) @Max(100) Integer oxygenSaturation,
	@NotNull @DecimalMin("34.0") @DecimalMax("42.0") BigDecimal temperature,
	@NotNull @Min(0) @Max(10) Integer pain,
	@NotNull @Min(3) @Max(15) Integer gcs,
	@NotBlank @Size(max = 500) String chiefComplaint
) {
}
