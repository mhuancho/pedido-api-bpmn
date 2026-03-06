package bpmn.pedido.app.service;

import bpmn.pedido.app.repository.entity.OutboxEventEntity;
import bpmn.pedido.app.repository.entity.PedidoEntity;

public interface OutboxService {
    void enqueueWorkflowMessage(Long pedidoId, String correlationKey, String messageName, String estado);
    void enqueueWorkflowStart(PedidoEntity pedido);
    long pendingCount();
    int cleanupProcessed();
    void publishBatch(int batchSize);
    void markProcessed(OutboxEventEntity event);
    void markFailed(OutboxEventEntity event, String error);
}
