-- Ejecutar en la base: ventas_db (schema public)
-- Objetivo: crear/asegurar todas las tablas, constraints e indices requeridos por la app.

CREATE TABLE IF NOT EXISTS public.pedido (
    id BIGSERIAL PRIMARY KEY,
    cliente VARCHAR(255) NOT NULL,
    monto INTEGER NOT NULL,
    estado VARCHAR(32) NOT NULL,
    process_instance_key BIGINT,
    process_definition_key BIGINT,
    pedido_correlation_key VARCHAR(255) NOT NULL UNIQUE,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pedido_estado ON public.pedido (estado);
CREATE INDEX IF NOT EXISTS idx_pedido_cliente ON public.pedido (cliente);

CREATE TABLE IF NOT EXISTS public.idempotency_record (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    accion VARCHAR(64) NOT NULL,
    pedido_id BIGINT NOT NULL,
    estado_resultante VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_idempotency_key_action_pedido'
    ) THEN
        ALTER TABLE public.idempotency_record
            ADD CONSTRAINT uk_idempotency_key_action_pedido
            UNIQUE (idempotency_key, accion, pedido_id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_idempotency_key
    ON public.idempotency_record (idempotency_key);

CREATE INDEX IF NOT EXISTS idx_idempotency_created_at
    ON public.idempotency_record (created_at);

CREATE TABLE IF NOT EXISTS public.outbox_event (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    message_name VARCHAR(64) NOT NULL,
    correlation_key VARCHAR(128) NOT NULL,
    estado VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL,
    last_error VARCHAR(512),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_next_attempt
    ON public.outbox_event (status, next_attempt_at);

CREATE TABLE IF NOT EXISTS public.pedido_evento (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES public.pedido (id),
    accion VARCHAR(255) NOT NULL,
    estado_anterior VARCHAR(32),
    estado_nuevo VARCHAR(32) NOT NULL,
    fecha_evento TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pedido_evento_pedido_fecha
    ON public.pedido_evento (pedido_id, fecha_evento);

-- Limpieza defensiva para dejar una sola fila por idempotency_key (si hubiera historico inconsistente)
DELETE FROM public.idempotency_record r
USING public.idempotency_record r2
WHERE r.idempotency_key = r2.idempotency_key
  AND r.id > r2.id;
