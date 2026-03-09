package bpmn.pedido.app.service.impl;

import bpmn.pedido.app.config.OrchestrationProperties;
import bpmn.pedido.app.config.WorkflowProperties;
import bpmn.pedido.app.repository.dao.OutboxEventRepository;
import bpmn.pedido.app.repository.dao.PedidoRepository;
import bpmn.pedido.app.repository.entity.OutboxEventEntity;
import bpmn.pedido.app.repository.entity.PedidoEntity;
import bpmn.pedido.app.model.enums.OutboxStatus;
import bpmn.pedido.app.service.gateway.CamundaGatewayClient;
import bpmn.pedido.app.service.gateway.dto.CorrelateMessageGatewayRequest;
import bpmn.pedido.app.service.gateway.dto.StartProcessGatewayRequest;
import bpmn.pedido.app.service.gateway.dto.StartProcessGatewayResponse;
import bpmn.pedido.app.service.OutboxService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static bpmn.pedido.app.utils.Constants.PEDIDO;
import static bpmn.pedido.app.utils.Constants.WORKFLOW_MESSAGE;
import static bpmn.pedido.app.utils.Constants.WORKFLOW_START;
import static bpmn.pedido.app.utils.Constants.PEDIDO_ID;
import static bpmn.pedido.app.utils.Constants.STATUS;
import static bpmn.pedido.app.utils.Constants.PEDIDO_NOT_FOUND;
import static bpmn.pedido.app.utils.Constants.PEDIDO_CORRELATION;
import static bpmn.pedido.app.utils.Constants.CLIENTE;
import static bpmn.pedido.app.utils.Constants.MONTO;
import static bpmn.pedido.app.utils.Constants.TIPO_EVENT_NOT_SUPPORT;
import static bpmn.pedido.app.utils.Constants.OUTBOX_EVENTS_PROCESSED_TOTAL;
import static bpmn.pedido.app.utils.Constants.EVENT_TYPE;
import static bpmn.pedido.app.utils.Constants.OUTBOX_EVENTS_FAILED_TOTAL;
import static bpmn.pedido.app.utils.Constants.OUTBOX_EVENTS_RETRY_TOTAL;

@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxServiceImpl.class);

    private final OutboxEventRepository outboxEventRepository;
    private final PedidoRepository pedidoRepository;
    private final CamundaGatewayClient camundaGatewayClient;
    private final MeterRegistry meterRegistry;
    private final TransactionTemplate transactionTemplate;
    private final WorkflowProperties workflowProperties;
    private final OrchestrationProperties orchestrationProperties;

    @Value("${app.outbox.max-retries:10}")
    private int maxRetries;

    @Value("${app.outbox.cleanup-retention-hours:168}")
    private long cleanupRetentionHours;

    @Transactional
    @Override
    public void enqueueWorkflowMessage(Long pedidoId, String correlationKey, String messageName, String estado) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setAggregateType(PEDIDO);
        event.setAggregateId(pedidoId);
        event.setEventType(WORKFLOW_MESSAGE);
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
        event.setAggregateType(PEDIDO);
        event.setAggregateId(pedido.getId());
        event.setEventType(WORKFLOW_START);
        event.setMessageName(workflowProperties.getProcess().getPedido());
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

        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        for (Long id : ids) {
            outboxEventRepository.findById(id).ifPresent(this::processEvent);
        }
    }

    private void processEvent(OutboxEventEntity event) {
        try {
            if (WORKFLOW_MESSAGE.equals(event.getEventType())) {
                camundaGatewayClient.correlateMessage(new CorrelateMessageGatewayRequest(
                        resolveMessageKey(event.getMessageName()),
                        event.getCorrelationKey(),
                        Map.of(
                                PEDIDO_ID, event.getAggregateId(),
                                STATUS, event.getEstado()
                        )
                ));
            } else if (WORKFLOW_START.equals(event.getEventType())) {
                PedidoEntity pedido = pedidoRepository.findById(event.getAggregateId())
                        .orElseThrow(() -> new IllegalStateException(PEDIDO_NOT_FOUND + event.getAggregateId()));

                StartProcessGatewayResponse pi = camundaGatewayClient.startProcess(
                        new StartProcessGatewayRequest(
                                orchestrationProperties.getKeys().getProcessPedido(),
                                event.getCorrelationKey(),
                                Map.of(
                                PEDIDO_ID, pedido.getId(),
                                PEDIDO_CORRELATION, pedido.getPedidoCorrelationKey(),
                                CLIENTE, pedido.getCliente(),
                                MONTO, pedido.getMonto(),
                                STATUS, pedido.getEstado().name()
                                )
                        )
                );

                pedido.setProcessInstanceKey(pi.instanceKey());
                pedido.setProcessDefinitionKey(pi.definitionKey());
                pedidoRepository.save(pedido);
            } else {
                throw new IllegalStateException(TIPO_EVENT_NOT_SUPPORT + event.getEventType());
            }

            markProcessed(event);
        } catch (Exception ex) {
            markFailed(event, ex.getMessage());
            log.error("Error publicando evento outbox id={} type={} message={}", event.getId(), event.getEventType(), ex.getMessage(), ex);
        }
    }

    @Override
   public void markProcessed(OutboxEventEntity event) {
        transactionTemplate.executeWithoutResult(status -> {
            event.setStatus(OutboxStatus.PROCESSED);
            event.setLastError(null);
            outboxEventRepository.save(event);
        });
        meterRegistry.counter(OUTBOX_EVENTS_PROCESSED_TOTAL, EVENT_TYPE, event.getEventType()).increment();
    }

    @Override
    public void markFailed(OutboxEventEntity event, String error) {
        transactionTemplate.executeWithoutResult(status -> {
            int retries = event.getRetryCount() + 1;
            event.setRetryCount(retries);
            event.setLastError(trim(error));
            if (retries >= maxRetries) {
                event.setStatus(OutboxStatus.FAILED);
                meterRegistry.counter(OUTBOX_EVENTS_FAILED_TOTAL, EVENT_TYPE, event.getEventType()).increment();
            } else {
                event.setStatus(OutboxStatus.PENDING);
                event.setNextAttemptAt(LocalDateTime.now().plusSeconds(Math.min(300, retries * 10L)));
                meterRegistry.counter(OUTBOX_EVENTS_RETRY_TOTAL, EVENT_TYPE, event.getEventType()).increment();
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

    private String resolveMessageKey(String messageName) {
        if (messageName.equals(workflowProperties.getMessages().getAprobadoPedido())) {
            return orchestrationProperties.getKeys().getMessagePedidoAprobado();
        }
        if (messageName.equals(workflowProperties.getMessages().getPagadoPedido())) {
            return orchestrationProperties.getKeys().getMessagePedidoPagado();
        }
        if (messageName.equals(workflowProperties.getMessages().getCanceladoPedido())) {
            return orchestrationProperties.getKeys().getMessagePedidoCancelado();
        }
        throw new IllegalStateException("messageName no soportado para gateway: " + messageName);
    }
}
