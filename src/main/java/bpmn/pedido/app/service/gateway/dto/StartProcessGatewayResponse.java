package bpmn.pedido.app.service.gateway.dto;

public record StartProcessGatewayResponse(
        long instanceKey,
        long definitionKey,
        String bpmnProcessId
) {
}
