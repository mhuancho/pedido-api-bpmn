package bpmn.pedido.app.service.gateway.dto;

import java.util.Map;

public record CorrelateMessageGatewayRequest(
        String messageKey,
        String correlationKey,
        Map<String, Object> variables
) {
}
