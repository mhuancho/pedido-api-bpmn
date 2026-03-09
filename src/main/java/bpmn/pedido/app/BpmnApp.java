package bpmn.pedido.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BpmnApp {

	public static void main(String[] args) {
		SpringApplication.run(BpmnApp.class, args);
	}

}
