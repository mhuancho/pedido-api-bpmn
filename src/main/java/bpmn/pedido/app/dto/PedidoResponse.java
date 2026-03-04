package bpmn.pedido.app.dto;

public record PedidoResponse(
        Long id,
        String cliente,
        Integer monto,
        String estado,
        Long processInstanceKey,
        Long processDefinitionKey
) {
}
