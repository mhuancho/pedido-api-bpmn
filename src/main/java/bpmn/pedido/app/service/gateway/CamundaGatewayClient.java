package bpmn.pedido.app.service.gateway;

import bpmn.pedido.app.service.gateway.dto.CorrelateMessageGatewayRequest;
import bpmn.pedido.app.service.gateway.dto.CorrelateMessageGatewayResponse;
import bpmn.pedido.app.service.gateway.dto.StartProcessGatewayRequest;
import bpmn.pedido.app.service.gateway.dto.StartProcessGatewayResponse;
import feign.Headers;
import feign.RequestLine;

public interface CamundaGatewayClient {

    @RequestLine("POST /internal/orchestration/process/start")
    @Headers("Content-Type: application/json")
    StartProcessGatewayResponse startProcess(StartProcessGatewayRequest request);

    @RequestLine("POST /internal/orchestration/message/correlate")
    @Headers("Content-Type: application/json")
    CorrelateMessageGatewayResponse correlateMessage(CorrelateMessageGatewayRequest request);
}
