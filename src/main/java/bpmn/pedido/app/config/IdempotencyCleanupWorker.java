package bpmn.pedido.app.config;

import bpmn.pedido.app.service.IdempotencyService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdempotencyCleanupWorker {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupWorker.class);

    private final IdempotencyService idempotencyService;

    @Scheduled(fixedDelayString = "${app.idempotency.cleanup-delay-ms:3600000}")
    @Observed(name = "idempotency.cleanup")
    public void cleanup() {
        int deleted = idempotencyService.cleanupExpired();
        if (deleted > 0) {
            log.info("Idempotency cleanup eliminados={}", deleted);
        }
    }
}
