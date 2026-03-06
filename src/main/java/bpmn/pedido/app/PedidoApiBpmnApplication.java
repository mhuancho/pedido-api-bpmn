package bpmn.pedido.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PedidoApiBpmnApplication {

	public static void main(String[] args) {
		SpringApplication.run(PedidoApiBpmnApplication.class, args);
	}

}
