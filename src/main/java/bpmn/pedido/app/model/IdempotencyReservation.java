package bpmn.pedido.app.model;

import bpmn.pedido.app.model.dto.CambioEstadoResponse;

public record IdempotencyReservation(
        boolean shouldProcess,
        CambioEstadoResponse response
) {
}
