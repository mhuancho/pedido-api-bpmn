package bpmn.pedido.app.config;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PedidoFinalizadoWorker {

    private static final Logger log = LoggerFactory.getLogger(PedidoFinalizadoWorker.class);

    @JobWorker(type = "notificar-pedido-finalizado")
    public void ejecutar(
            @Variable Long pedidoId,
            @Variable String estado
    ) {
        log.info("Pedido finalizado. pedidoId={} estado={}", pedidoId, estado);
    }
}