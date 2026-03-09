package bpmn.pedido.app.config;

import bpmn.pedido.app.service.gateway.CamundaGatewayClient;
import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.Util;
import feign.codec.ErrorDecoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignGatewayConfig {

    @Bean
    CamundaGatewayClient camundaGatewayClient(OrchestrationProperties orchestrationProperties) {
        return Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .logger(new Slf4jLogger(CamundaGatewayClient.class))
                .logLevel(Logger.Level.BASIC)
                .options(requestOptions(orchestrationProperties))
                .retryer(retryer(orchestrationProperties))
                .requestInterceptor(internalTokenInterceptor(orchestrationProperties))
                .requestInterceptor(correlationIdInterceptor())
                .errorDecoder(errorDecoder())
                .target(CamundaGatewayClient.class, orchestrationProperties.getBaseUrl());
    }

    @Bean
    ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            String body = "";
            if (response.body() != null) {
                try {
                    body = Util.toString(response.body().asReader(StandardCharsets.UTF_8));
                } catch (IOException ignored) {
                    body = "";
                }
            }
            return new IllegalStateException("Error llamando camunda-gateway-api en " + methodKey +
                    " status=" + response.status() + " body=" + body);
        };
    }

    @Bean
    Request.Options requestOptions(OrchestrationProperties orchestrationProperties) {
        return new Request.Options(
                orchestrationProperties.getConnectTimeoutMs(),
                TimeUnit.MILLISECONDS,
                orchestrationProperties.getReadTimeoutMs(),
                TimeUnit.MILLISECONDS,
                true
        );
    }

    @Bean
    Retryer retryer(OrchestrationProperties orchestrationProperties) {
        return new Retryer.Default(
                orchestrationProperties.getRetryPeriodMs(),
                orchestrationProperties.getRetryMaxPeriodMs(),
                orchestrationProperties.getRetryMaxAttempts()
        );
    }

    @Bean
    RequestInterceptor internalTokenInterceptor(OrchestrationProperties orchestrationProperties) {
        return template -> template.header(
                orchestrationProperties.getSecurity().getTokenHeader(),
                orchestrationProperties.getSecurity().getToken()
        );
    }

    @Bean
    RequestInterceptor correlationIdInterceptor() {
        return template -> {
            String correlationId = MDC.get("traceId");
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            template.header("X-Correlation-Id", correlationId);
        };
    }
}
