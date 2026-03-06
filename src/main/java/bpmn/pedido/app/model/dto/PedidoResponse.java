package bpmn.pedido.app.model.dto;

import java.time.LocalDateTime;

public record PedidoResponse(
        Long id,
        String cliente,
        Integer monto,
        String estado,
        Long processInstanceKey,
        Long processDefinitionKey,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}
