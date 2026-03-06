package bpmn.pedido.app.service;

import bpmn.pedido.app.model.IdempotencyReservation;
import bpmn.pedido.app.model.dto.CambioEstadoResponse;

public interface IdempotencyService {
    void validateNoCrossReuse(String key, String accion, Long pedidoId);
    IdempotencyReservation reserve(String key, String accion, CambioEstadoResponse response);
    int cleanupExpired();
}
