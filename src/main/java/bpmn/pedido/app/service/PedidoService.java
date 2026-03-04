package bpmn.pedido.app.service;

import bpmn.pedido.app.dao.PedidoRepository;
import bpmn.pedido.app.dto.CambioEstadoResponse;
import bpmn.pedido.app.dto.CrearPedidoRequest;
import bpmn.pedido.app.dto.PedidoResponse;
import bpmn.pedido.app.entity.PedidoEntity;
import bpmn.pedido.app.enums.EstadoPedido;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final CamundaClient camundaClient;

    @Transactional
    public PedidoResponse iniciar(CrearPedidoRequest request) {
        PedidoEntity pedido = new PedidoEntity();
        pedido.setCliente(request.cliente());
        pedido.setMonto(request.monto());
        pedido.setEstado(EstadoPedido.PENDIENTE);

        // usar una correlation key estable y explícita
        pedido.setPedidoCorrelationKey(java.util.UUID.randomUUID().toString());

        pedido = pedidoRepository.save(pedido);

        ProcessInstanceEvent event = camundaClient
                .newCreateInstanceCommand()
                .bpmnProcessId("proceso-pedido")
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

        pedido.setProcessInstanceKey(event.getProcessInstanceKey());
        pedido.setProcessDefinitionKey(event.getProcessDefinitionKey());

        pedidoRepository.save(pedido);

        return toResponse(pedido);
    }

    @Transactional
    public CambioEstadoResponse aprobar(Long id) {
        PedidoEntity pedido = getPedido(id);

        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new IllegalStateException("Solo se puede aprobar un pedido en estado PENDIENTE");
        }

        pedido.setEstado(EstadoPedido.APROBADO);
        pedidoRepository.save(pedido);

        camundaClient.newCorrelateMessageCommand()
                .messageName("pedido-aprobado")
                .correlationKey(pedido.getPedidoCorrelationKey())
                .variables(Map.of(
                        "pedidoId", pedido.getId(),
                        "estado", pedido.getEstado().name()
                ))
                .send()
                .join();

        return new CambioEstadoResponse(pedido.getId(), pedido.getEstado().name());
    }

    @Transactional
    public CambioEstadoResponse pagar(Long id) {
        PedidoEntity pedido = getPedido(id);

        if (pedido.getEstado() != EstadoPedido.APROBADO) {
            throw new IllegalStateException("Solo se puede pagar un pedido en estado APROBADO");
        }

        pedido.setEstado(EstadoPedido.PAGADO);
        pedidoRepository.save(pedido);

        camundaClient.newCorrelateMessageCommand()
                .messageName("pedido-pagado")
                .correlationKey(pedido.getPedidoCorrelationKey())
                .variables(Map.of(
                        "pedidoId", pedido.getId(),
                        "estado", pedido.getEstado().name()
                ))
                .send()
                .join();

        return new CambioEstadoResponse(pedido.getId(), pedido.getEstado().name());
    }

    @Transactional
    public CambioEstadoResponse cancelar(Long id) {
        PedidoEntity pedido = getPedido(id);

        if (pedido.getEstado() == EstadoPedido.PAGADO || pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new IllegalStateException("No se puede cancelar un pedido ya finalizado");
        }

        pedido.setEstado(EstadoPedido.CANCELADO);
        pedidoRepository.save(pedido);

        camundaClient.newCorrelateMessageCommand()
                .messageName("pedido-cancelado")
                .correlationKey(pedido.getPedidoCorrelationKey())
                .variables(Map.of(
                        "pedidoId", pedido.getId(),
                        "estado", pedido.getEstado().name()
                ))
                .send()
                .join();

        return new CambioEstadoResponse(pedido.getId(), pedido.getEstado().name());
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long id) {
        return toResponse(getPedido(id));
    }

    private PedidoEntity getPedido(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado: " + id));
    }

    private PedidoResponse toResponse(PedidoEntity pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getCliente(),
                pedido.getMonto(),
                pedido.getEstado().name(),
                pedido.getProcessInstanceKey(),
                pedido.getProcessDefinitionKey()
        );
    }
}