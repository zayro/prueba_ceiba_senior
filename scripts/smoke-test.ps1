param(
	[string]$TriageUrl,
	[string]$AssistantUrl = "http://localhost:8081",
	[string]$ApiKey
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($TriageUrl)) {
	$triageHostPort = $env:TRIAGE_HOST_PORT
	if ([string]::IsNullOrWhiteSpace($triageHostPort) -and (Test-Path ".env")) {
		$triagePortLine = Get-Content ".env" |
			Where-Object { $_ -match '^\s*TRIAGE_HOST_PORT\s*=' } |
			Select-Object -First 1
		if ($null -ne $triagePortLine) {
			$triageHostPort = ($triagePortLine -split "=", 2)[1].Trim().Trim('"').Trim("'")
		}
	}
	if ([string]::IsNullOrWhiteSpace($triageHostPort)) {
		$triageHostPort = "18080"
	}
	$TriageUrl = "http://localhost:$triageHostPort"
}

if ([string]::IsNullOrWhiteSpace($ApiKey)) {
	if (!(Test-Path ".env")) {
		throw "No se encontró .env. Ejecuta make setup y configura API_KEY."
	}
	$apiKeyLine = Get-Content ".env" |
		Where-Object { $_ -match '^\s*API_KEY\s*=' } |
		Select-Object -First 1
	if ($null -eq $apiKeyLine) {
		throw "No se encontró API_KEY en .env."
	}
	$ApiKey = ($apiKeyLine -split "=", 2)[1].Trim().Trim('"').Trim("'")
	if ([string]::IsNullOrWhiteSpace($ApiKey) -or $ApiKey -eq "replace-with-a-random-api-key") {
		throw "Configura API_KEY en .env antes de ejecutar el smoke test."
	}
}

$headers = @{ "X-API-Key" = $ApiKey }

function Assert-Equal($Actual, $Expected, [string]$Message) {
	if ($Actual -ne $Expected) {
		throw "$Message (esperado '$Expected', recibido '$Actual')."
	}
}

function Get-HttpStatusCode([scriptblock]$Action) {
	try {
		& $Action | Out-Null
		throw "Se esperaba un error HTTP, pero la petición tuvo éxito."
	} catch {
		if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
			return [int]$_.Exception.Response.StatusCode
		}
		throw
	}
}

$servicesReady = $false
for ($attempt = 0; $attempt -lt 60; $attempt++) {
	try {
		$triageHealth = Invoke-RestMethod "$TriageUrl/actuator/health/readiness" -TimeoutSec 3
		$assistantHealth = Invoke-RestMethod "$AssistantUrl/actuator/health/readiness" -TimeoutSec 3
		if ($triageHealth.status -eq "UP" -and $assistantHealth.status -eq "UP") {
			$servicesReady = $true
			break
		}
	} catch {
		Start-Sleep -Seconds 2
	}
	if (!$servicesReady) {
		Start-Sleep -Seconds 2
	}
}
if (!$servicesReady) {
	throw "Los servicios no quedaron listos en 2 minutos. Revisa 'make logs'."
}
Assert-Equal $triageHealth.status "UP" "Readiness de Triage"
Assert-Equal $assistantHealth.status "UP" "Readiness del asistente"
Write-Host "[OK] Ambos servicios están listos."

$baselineCounts = Invoke-RestMethod "$AssistantUrl/asistente/estado-urgencias" -Headers $headers
$patientRequest = @{
	fullName = "Paciente Smoke Test"
	age = 45
	heartRate = 130
	systolicPressure = 180
	diastolicPressure = 110
	oxygenSaturation = 97
	temperature = 36.8
	pain = 8
	gcs = 15
	chiefComplaint = "Dolor torácico"
} | ConvertTo-Json
$patient = Invoke-RestMethod -Method Post -Uri "$TriageUrl/triage/pacientes" `
	-Headers $headers -ContentType "application/json" -Body $patientRequest
Assert-Equal $patient.level "CRITICO" "Clasificación E1"
Assert-Equal $patient.state "REGISTRADO" "Estado inicial"
if ($patient.slaMinutes -le 0) {
	throw "El paciente no tiene un SLA positivo."
}
Write-Host "[OK] Registro y clasificación CRITICO. ID: $($patient.id)"

$projectionUpdated = $false
for ($attempt = 0; $attempt -lt 20; $attempt++) {
	Start-Sleep -Milliseconds 500
	$counts = Invoke-RestMethod "$AssistantUrl/asistente/estado-urgencias" -Headers $headers
	if ($counts.CRITICO -gt $baselineCounts.CRITICO) {
		$projectionUpdated = $true
		break
	}
}
if (!$projectionUpdated) {
	throw "El evento Kafka no actualizó a tiempo la proyección Redis del asistente."
}
Write-Host "[OK] Evento Kafka proyectado en Redis."

$stateRequest = @{ state = "EN_CONSULTA" } | ConvertTo-Json
$consultation = Invoke-RestMethod -Method Patch `
	-Uri "$TriageUrl/triage/pacientes/$($patient.id)/estado" `
	-Headers $headers -ContentType "application/json" -Body $stateRequest
Assert-Equal $consultation.state "EN_CONSULTA" "Transición a consulta"

$dischargeRequest = @{ state = "ALTA" } | ConvertTo-Json
$discharged = Invoke-RestMethod -Method Patch `
	-Uri "$TriageUrl/triage/pacientes/$($patient.id)/estado" `
	-Headers $headers -ContentType "application/json" -Body $dischargeRequest
Assert-Equal $discharged.state "ALTA" "Transición a alta"

$invalidTransitionStatus = Get-HttpStatusCode {
	Invoke-RestMethod -Method Patch `
		-Uri "$TriageUrl/triage/pacientes/$($patient.id)/estado" `
		-Headers $headers -ContentType "application/json" -Body $stateRequest
}
Assert-Equal $invalidTransitionStatus 409 "Transición inválida"
Write-Host "[OK] Transiciones válidas e inválida (409)."

$patientDetails = Invoke-RestMethod "$TriageUrl/triage/pacientes/$($patient.id)" -Headers $headers
Assert-Equal $patientDetails.state "ALTA" "Estado persistido"
if (@($patientDetails.audit).Count -lt 3) {
	throw "Faltan entradas de auditoría para el paciente."
}
Write-Host "[OK] Estado persistido y auditoría clínica disponible."

$from = [Uri]::EscapeDataString([DateTime]::UtcNow.AddMinutes(-2).ToString("o"))
$until = [Uri]::EscapeDataString([DateTime]::UtcNow.AddMinutes(2).ToString("o"))
$report = Invoke-RestMethod `
	"$TriageUrl/triage/reportes/operacion?desde=$from&hasta=$until" -Headers $headers
if (@($report | Where-Object { $_.critico -gt 0 }).Count -eq 0) {
	throw "El reporte SQL no contiene el paciente registrado durante la prueba."
}
Write-Host "[OK] Reporte operativo."

$noMatchRequest = @{ pregunta = "Tratamiento de primera línea para diabetes tipo 2" } |
	ConvertTo-Json
$noMatch = Invoke-RestMethod -Method Post -Uri "$AssistantUrl/asistente/consulta" `
	-Headers $headers -ContentType "application/json" -Body $noMatchRequest
Assert-Equal $noMatch.respuesta "No existe un protocolo disponible para la pregunta." `
	"Respuesta RAG sin coincidencia"
Assert-Equal @($noMatch.fuentes).Count 0 "Fuentes para una consulta sin coincidencia"
Write-Host "[OK] RAG sin coincidencia: rechazo explícito, sin generación clínica."

$unauthorizedStatus = Get-HttpStatusCode {
	Invoke-RestMethod "$AssistantUrl/asistente/estado-urgencias"
}
Assert-Equal $unauthorizedStatus 401 "Protección API key"
Write-Host "[OK] Autenticación por API key."

Write-Host ""
Write-Host "Smoke test completado correctamente."
