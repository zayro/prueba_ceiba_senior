CREATE TABLE IF NOT EXISTS patients (
    id UUID PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    age INTEGER NOT NULL CHECK (age BETWEEN 0 AND 120),
    heart_rate INTEGER NOT NULL CHECK (heart_rate BETWEEN 20 AND 250),
    systolic_pressure INTEGER NOT NULL CHECK (systolic_pressure BETWEEN 60 AND 260),
    diastolic_pressure INTEGER NOT NULL CHECK (diastolic_pressure BETWEEN 30 AND 160),
    oxygen_saturation INTEGER NOT NULL CHECK (oxygen_saturation BETWEEN 70 AND 100),
    temperature NUMERIC(4, 2) NOT NULL CHECK (temperature BETWEEN 34.0 AND 42.0),
    pain INTEGER NOT NULL CHECK (pain BETWEEN 0 AND 10),
    gcs INTEGER NOT NULL CHECK (gcs BETWEEN 3 AND 15),
    chief_complaint VARCHAR(500) NOT NULL,
    level VARCHAR(20) NOT NULL,
    sla_minutes INTEGER NOT NULL CHECK (sla_minutes > 0),
    state VARCHAR(20) NOT NULL,
    escalated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    state_since TIMESTAMPTZ NOT NULL,
    first_consultation_at TIMESTAMPTZ,
    CHECK (diastolic_pressure < systolic_pressure)
);

CREATE INDEX IF NOT EXISTS patients_state_created_idx ON patients(state, created_at);
CREATE INDEX IF NOT EXISTS patients_created_level_idx ON patients(created_at, level);

CREATE TABLE IF NOT EXISTS clinical_audit (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patients(id),
    occurred_at TIMESTAMPTZ NOT NULL,
    previous_state VARCHAR(20),
    new_state VARCHAR(20) NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    previous_escalated BOOLEAN NOT NULL DEFAULT FALSE,
    new_escalated BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS clinical_audit_patient_time_idx
    ON clinical_audit(patient_id, occurred_at);

CREATE TABLE IF NOT EXISTS event_outbox (
    id UUID PRIMARY KEY,
    aggregate_key VARCHAR(36) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS event_outbox_pending_idx
    ON event_outbox(created_at) WHERE published_at IS NULL;
