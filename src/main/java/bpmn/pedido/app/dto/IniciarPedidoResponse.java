package bpmn.pedido.app.dto;

public record IniciarPedidoResponse(
        long processInstanceKey,
        long processDefinitionKey
) {
}