package bpmn.pedido.app.config;

import bpmn.pedido.app.service.OutboxService;
import bpmn.pedido.app.service.impl.OutboxServiceImpl;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class OutboxPublisherWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherWorker.class);

    private final OutboxService outboxService;
    private final AtomicLong pendingGauge = new AtomicLong(0);

    public OutboxPublisherWorker(OutboxService outboxService, MeterRegistry meterRegistry) {
        this.outboxService = outboxService;
        meterRegistry.gauge("outbox.events.pending", pendingGauge);
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher-delay-ms:3000}")
    @Observed(name = "outbox.publish.batch")
    public void publishPendingEvents() {
        outboxService.publishBatch(100);
        pendingGauge.set(outboxService.pendingCount());
    }

    @Scheduled(fixedDelayString = "${app.outbox.cleanup-delay-ms:3600000}")
    @Observed(name = "outbox.cleanup")
    public void cleanupProcessedEvents() {
        int deleted = outboxService.cleanupProcessed();
        if (deleted > 0) {
            log.info("Outbox cleanup eliminados={}", deleted);
        }
    }
}
