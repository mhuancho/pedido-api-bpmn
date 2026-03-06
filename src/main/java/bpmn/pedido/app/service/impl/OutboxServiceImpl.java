package bpmn.pedido.app.service.impl;

import bpmn.pedido.app.repository.dao.OutboxEventRepository;
import bpmn.pedido.app.repository.dao.PedidoRepository;
import bpmn.pedido.app.repository.entity.OutboxEventEntity;
import bpmn.pedido.app.repository.entity.PedidoEntity;
import bpmn.pedido.app.model.enums.OutboxStatus;
import bpmn.pedido.app.service.OutboxService;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxServiceImpl.class);

    private final OutboxEventRepository outboxEventRepository;
    private final PedidoRepository pedidoRepository;
    private final CamundaClient camundaClient;
    private final MeterRegistry meterRegistry;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.outbox.max-retries:10}")
    private int maxRetries;

    @Value("${app.outbox.cleanup-retention-hours:168}")
    private long cleanupRetentionHours;

    @Transactional
    @Override
    public void enqueueWorkflowMessage(Long pedidoId, String correlationKey, String messageName, String estado) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setAggregateType("PEDIDO");
        event.setAggregateId(pedidoId);
        event.setEventType("WORKFLOW_MESSAGE");
        event.setMessageName(messageName);
        event.setCorrelationKey(correlationKey);
        event.setEstado(estado);
        event.setStatus(OutboxStatus.PENDING);
        event.setRetryCount(0);
        event.setNextAttemptAt(LocalDateTime.now());
        outboxEventRepository.save(event);
    }

    @Transactional
    @Override
    public void enqueueWorkflowStart(PedidoEntity pedido) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setAggregateType("PEDIDO");
        event.setAggregateId(pedido.getId());
        event.setEventType("WORKFLOW_START");
        event.setMessageName("proceso-pedido");
        event.setCorrelationKey(pedido.getPedidoCorrelationKey());
        event.setEstado(pedido.getEstado().name());
        event.setStatus(OutboxStatus.PENDING);
        event.setRetryCount(0);
        event.setNextAttemptAt(LocalDateTime.now());
        outboxEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    @Override
    public long pendingCount() {
        return outboxEventRepository.countByStatus(OutboxStatus.PENDING);
    }

    @Transactional
    @Override
    public int cleanupProcessed() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(cleanupRetentionHours);
        return (int) outboxEventRepository.deleteByStatusAndUpdatedAtBefore(OutboxStatus.PROCESSED, cutoff);
    }

    public void publishBatch(int batchSize) {
        List<Long> ids = transactionTemplate.execute(status -> {
            List<Long> claimIds = outboxEventRepository.findIdsToClaim(LocalDateTime.now(), batchSize);
            if (claimIds.isEmpty()) {
                return claimIds;
            }
            outboxEventRepository.updateStatusForIds(claimIds, OutboxStatus.PENDING, OutboxStatus.PROCESSING);
            return claimIds;
        });

        if (ids == null || ids.isEmpty()) {
            return;
        }

        for (Long id : ids) {
            outboxEventRepository.findById(id).ifPresent(this::processEvent);
        }
    }

    private void processEvent(OutboxEventEntity event) {
        try {
            if ("WORKFLOW_MESSAGE".equals(event.getEventType())) {
                camundaClient.newCorrelateMessageCommand()
                        .messageName(event.getMessageName())
                        .correlationKey(event.getCorrelationKey())
                        .variables(Map.of(
                                "pedidoId", event.getAggregateId(),
                                "estado", event.getEstado()
                        ))
                        .send()
                        .join();
            } else if ("WORKFLOW_START".equals(event.getEventType())) {
                PedidoEntity pedido = pedidoRepository.findById(event.getAggregateId())
                        .orElseThrow(() -> new IllegalStateException("Pedido no encontrado para outbox: " + event.getAggregateId()));

                ProcessInstanceEvent pi = camundaClient
                        .newCreateInstanceCommand()
                        .bpmnProcessId(event.getMessageName())
                        .latestVersion()
                        .variables(Map.of(
                                "pedidoId", pedido.getId(),
                                "pedidoCorrelationKey", pedido.getPedidoCorrelationKey(),
                                "cliente", pedido.getCliente(),
                                "monto", pedido.getMonto(),
                                "estado", pedido.getEstado().name()
                        ))
                        .send()
                        .join();

                pedido.setProcessInstanceKey(pi.getProcessInstanceKey());
                pedido.setProcessDefinitionKey(pi.getProcessDefinitionKey());
                pedidoRepository.save(pedido);
            } else {
                throw new IllegalStateException("Tipo de evento outbox no soportado: " + event.getEventType());
            }

            markProcessed(event);
        } catch (Exception ex) {
            markFailed(event, ex.getMessage());
            log.error("Error publicando evento outbox id={} type={} message={}", event.getId(), event.getEventType(), ex.getMessage());
        }
    }

    @Override
   public void markProcessed(OutboxEventEntity event) {
        transactionTemplate.executeWithoutResult(status -> {
            event.setStatus(OutboxStatus.PROCESSED);
            event.setLastError(null);
            outboxEventRepository.save(event);
        });
        meterRegistry.counter("outbox.events.processed.total", "eventType", event.getEventType()).increment();
    }

    @Override
    public void markFailed(OutboxEventEntity event, String error) {
        transactionTemplate.executeWithoutResult(status -> {
            int retries = event.getRetryCount() + 1;
            event.setRetryCount(retries);
            event.setLastError(trim(error));
            if (retries >= maxRetries) {
                event.setStatus(OutboxStatus.FAILED);
                meterRegistry.counter("outbox.events.failed.total", "eventType", event.getEventType()).increment();
            } else {
                event.setStatus(OutboxStatus.PENDING);
                event.setNextAttemptAt(LocalDateTime.now().plusSeconds(Math.min(300, retries * 10L)));
                meterRegistry.counter("outbox.events.retry.total", "eventType", event.getEventType()).increment();
            }
            outboxEventRepository.save(event);
        });
    }

    private String trim(String text) {
        if (text == null) {
            return null;
        }
        if (text.length() <= 500) {
            return text;
        }
        return text.substring(0, 500);
    }
}
