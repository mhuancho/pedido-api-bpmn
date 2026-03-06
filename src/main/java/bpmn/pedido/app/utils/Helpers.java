package bpmn.pedido.app.utils;

import bpmn.pedido.app.exception.IdempotencyKeyRequiredException;

public class Helpers {

    private Helpers(){}

    public static String requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IdempotencyKeyRequiredException("El header Idempotency-Key es obligatorio");
        }
        return idempotencyKey.trim();
    }
}
