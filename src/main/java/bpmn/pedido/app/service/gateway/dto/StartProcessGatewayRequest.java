package bpmn.pedido.app.service.gateway.dto;

import java.util.Map;

public record StartProcessGatewayRequest(
        String processKey,
        String businessKey,
        Map<String, Object> variables
) {
}
