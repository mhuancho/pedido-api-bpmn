package bpmn.pedido.app.model.dto;

import java.time.LocalDateTime;

public record PedidoEventoResponse(
        Long id,
        String accion,
        String estadoAnterior,
        String estadoNuevo,
        LocalDateTime fechaEvento
) {
}
