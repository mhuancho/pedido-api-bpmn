package bpmn.pedido.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.workflow")
public class WorkflowProperties {
    private Process process = new Process();
    private Messages messages = new Messages();

    @Getter
    @Setter
    public static class Process {
        private String pedido;
    }

    @Getter
    @Setter
    public static class Messages {
        private String aprobadoPedido;
        private String pagadoPedido;
        private String canceladoPedido;
    }
}
