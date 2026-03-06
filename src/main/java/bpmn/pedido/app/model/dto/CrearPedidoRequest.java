package bpmn.pedido.app.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CrearPedidoRequest(
        @NotBlank String cliente,
        @NotNull @Positive Integer monto
) {
}
