package bpmn.pedido.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.orchestration")
public class OrchestrationProperties {
    private String baseUrl;
    private long connectTimeoutMs = 3000;
    private long readTimeoutMs = 5000;
    private int retryMaxAttempts = 2;
    private long retryPeriodMs = 200;
    private long retryMaxPeriodMs = 1000;
    private Keys keys = new Keys();
    private Security security = new Security();

    @Getter
    @Setter
    public static class Keys {
        private String processPedido;
        private String messagePedidoAprobado;
        private String messagePedidoPagado;
        private String messagePedidoCancelado;
    }

    @Getter
    @Setter
    public static class Security {
        private String tokenHeader = "X-Internal-Token";
        private String token;
    }
}
