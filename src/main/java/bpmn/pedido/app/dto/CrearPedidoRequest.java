package bpmn.pedido.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CrearPedidoRequest(
        @NotBlank String cliente,
        @NotNull Integer monto
) {
}