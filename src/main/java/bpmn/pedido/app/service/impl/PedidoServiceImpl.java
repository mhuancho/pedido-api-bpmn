package bpmn.pedido.app.service.impl;

import bpmn.pedido.app.repository.dao.PedidoEventoRepository;
import bpmn.pedido.app.repository.dao.PedidoRepository;
import bpmn.pedido.app.model.dto.CambioEstadoResponse;
import bpmn.pedido.app.model.dto.CrearPedidoRequest;
import bpmn.pedido.app.model.dto.PedidoEventoResponse;
import bpmn.pedido.app.model.dto.PedidoResponse;
import bpmn.pedido.app.repository.entity.PedidoEntity;
import bpmn.pedido.app.repository.entity.PedidoEventoEntity;
import bpmn.pedido.app.model.enums.EstadoPedido;
import bpmn.pedido.app.service.IdempotencyService;
import bpmn.pedido.app.service.OutboxService;
import bpmn.pedido.app.service.PedidoService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoEventoRepository pedidoEventoRepository;
    private final IdempotencyService idempotencyService;
    private final OutboxService outboxService;
    private final MeterRegistry meterRegistry;

    @Transactional
    @Override
    public PedidoResponse iniciar(CrearPedidoRequest request) {
        PedidoEntity pedido = new PedidoEntity();
        pedido.setCliente(request.cliente());
        pedido.setMonto(request.monto());
        pedido.setEstado(EstadoPedido.PENDIENTE);

        pedido.setPedidoCorrelationKey(UUID.randomUUID().toString());

        pedido = pedidoRepository.save(pedido);

        outboxService.enqueueWorkflowStart(pedido);
        registrarEvento(pedido, "INICIAR_PEDIDO", null, EstadoPedido.PENDIENTE);
        meterRegistry.counter("pedido.transition.total", "accion", "INICIAR_PEDIDO", "estado", EstadoPedido.PENDIENTE.name()).increment();

        return toResponse(pedido);
    }

    @Transactional
    @Override
    public CambioEstadoResponse aprobar(Long id, String idempotencyKey) {
        final String accion = "APROBAR_PEDIDO";
        PedidoEntity pedido = getPedidoForUpdate(id);
        idempotencyService.validateNoCrossReuse(idempotencyKey, accion, id);
        EstadoPedido estadoAnterior = pedido.getEstado();

        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new IllegalStateException("Solo se puede aprobar un pedido en estado PENDIENTE");
        }

        CambioEstadoResponse expectedResponse = new CambioEstadoResponse(pedido.getId(), EstadoPedido.APROBADO.name());
        var reservation = idempotencyService.reserve(idempotencyKey, accion, expectedResponse);
        if (!reservation.shouldProcess()) {
            return reservation.response();
        }

        pedido.setEstado(EstadoPedido.APROBADO);
        pedidoRepository.save(pedido);

        outboxService.enqueueWorkflowMessage(
                pedido.getId(),
                pedido.getPedidoCorrelationKey(),
                "pedido-aprobado",
                pedido.getEstado().name()
        );

        registrarEvento(pedido, accion, estadoAnterior, EstadoPedido.APROBADO);
        meterRegistry.counter("pedido.transition.total", "accion", accion, "estado", EstadoPedido.APROBADO.name()).increment();
        return expectedResponse;
    }

    @Transactional
    @Override
    public CambioEstadoResponse pagar(Long id, String idempotencyKey) {
        final String accion = "PAGAR_PEDIDO";
        PedidoEntity pedido = getPedidoForUpdate(id);
        idempotencyService.validateNoCrossReuse(idempotencyKey, accion, id);
        EstadoPedido estadoAnterior = pedido.getEstado();

        if (pedido.getEstado() != EstadoPedido.APROBADO) {
            throw new IllegalStateException("Solo se puede pagar un pedido en estado APROBADO");
        }

        CambioEstadoResponse expectedResponse = new CambioEstadoResponse(pedido.getId(), EstadoPedido.PAGADO.name());
        var reservation = idempotencyService.reserve(idempotencyKey, accion, expectedResponse);
        if (!reservation.shouldProcess()) {
            return reservation.response();
        }

        pedido.setEstado(EstadoPedido.PAGADO);
        pedidoRepository.save(pedido);

        outboxService.enqueueWorkflowMessage(
                pedido.getId(),
                pedido.getPedidoCorrelationKey(),
                "pedido-pagado",
                pedido.getEstado().name()
        );

        registrarEvento(pedido, accion, estadoAnterior, EstadoPedido.PAGADO);
        meterRegistry.counter("pedido.transition.total", "accion", accion, "estado", EstadoPedido.PAGADO.name()).increment();
        return expectedResponse;
    }

    @Transactional
    @Override
    public CambioEstadoResponse cancelar(Long id, String idempotencyKey) {
        final String accion = "CANCELAR_PEDIDO";
        PedidoEntity pedido = getPedidoForUpdate(id);
        idempotencyService.validateNoCrossReuse(idempotencyKey, accion, id);
        EstadoPedido estadoAnterior = pedido.getEstado();

        if (pedido.getEstado() == EstadoPedido.PAGADO || pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new IllegalStateException("No se puede cancelar un pedido ya finalizado");
        }

        CambioEstadoResponse expectedResponse = new CambioEstadoResponse(pedido.getId(), EstadoPedido.CANCELADO.name());
        var reservation = idempotencyService.reserve(idempotencyKey, accion, expectedResponse);
        if (!reservation.shouldProcess()) {
            return reservation.response();
        }

        pedido.setEstado(EstadoPedido.CANCELADO);
        pedidoRepository.save(pedido);

        outboxService.enqueueWorkflowMessage(
                pedido.getId(),
                pedido.getPedidoCorrelationKey(),
                "pedido-cancelado",
                pedido.getEstado().name()
        );

        registrarEvento(pedido, accion, estadoAnterior, EstadoPedido.CANCELADO);
        meterRegistry.counter("pedido.transition.total", "accion", accion, "estado", EstadoPedido.CANCELADO.name()).increment();
        return expectedResponse;
    }

    @Transactional(readOnly = true)
    @Override
    public PedidoResponse obtener(Long id) {
        return toResponse(getPedido(id));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<PedidoResponse> listar(String cliente, EstadoPedido estado, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return pedidoRepository.findAll((root, query, cb) -> {
            var predicates = cb.conjunction();
            if (cliente != null && !cliente.isBlank()) {
                predicates = cb.and(predicates, cb.like(cb.lower(root.get("cliente")), "%" + cliente.toLowerCase() + "%"));
            }
            if (estado != null) {
                predicates = cb.and(predicates, cb.equal(root.get("estado"), estado));
            }
            return predicates;
        }, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    @Override
    public List<PedidoEventoResponse> historial(Long pedidoId) {
        if (!pedidoRepository.existsById(pedidoId)) {
            throw new EntityNotFoundException("Pedido no encontrado: " + pedidoId);
        }

        return pedidoEventoRepository.findByPedidoIdOrderByFechaEventoAsc(pedidoId)
                .stream()
                .map(evento -> new PedidoEventoResponse(
                        evento.getId(),
                        evento.getAccion(),
                        evento.getEstadoAnterior() != null ? evento.getEstadoAnterior().name() : null,
                        evento.getEstadoNuevo().name(),
                        evento.getFechaEvento()
                ))
                .toList();
    }

    private PedidoEntity getPedido(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado: " + id));
    }

    private PedidoEntity getPedidoForUpdate(Long id) {
        return pedidoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado: " + id));
    }

    private void registrarEvento(PedidoEntity pedido, String accion, EstadoPedido estadoAnterior, EstadoPedido estadoNuevo) {
        PedidoEventoEntity evento = new PedidoEventoEntity();
        evento.setPedido(pedido);
        evento.setAccion(accion);
        evento.setEstadoAnterior(estadoAnterior);
        evento.setEstadoNuevo(estadoNuevo);
        pedidoEventoRepository.save(evento);
    }

    private PedidoResponse toResponse(PedidoEntity pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getCliente(),
                pedido.getMonto(),
                pedido.getEstado().name(),
                pedido.getProcessInstanceKey(),
                pedido.getProcessDefinitionKey(),
                pedido.getFechaCreacion(),
                pedido.getFechaActualizacion()
        );
    }
}
