package bpmn.pedido.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record IniciarPedidoRequest(
        @NotBlank String cliente,
        @NotNull Integer monto
) {
}