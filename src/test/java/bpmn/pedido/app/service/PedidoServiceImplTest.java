package bpmn.pedido.app.service;

import bpmn.pedido.app.config.OrchestrationProperties;
import bpmn.pedido.app.config.WorkflowProperties;
import bpmn.pedido.app.repository.dao.PedidoEventoRepository;
import bpmn.pedido.app.repository.dao.PedidoRepository;
import bpmn.pedido.app.model.dto.CambioEstadoResponse;
import bpmn.pedido.app.model.dto.CrearPedidoRequest;
import bpmn.pedido.app.model.dto.PedidoEventoResponse;
import bpmn.pedido.app.repository.entity.PedidoEventoEntity;
import bpmn.pedido.app.repository.entity.PedidoEntity;
import bpmn.pedido.app.model.enums.EstadoPedido;
import bpmn.pedido.app.model.IdempotencyReservation;
import bpmn.pedido.app.service.gateway.CamundaGatewayClient;
import bpmn.pedido.app.service.gateway.dto.StartProcessGatewayResponse;
import bpmn.pedido.app.service.impl.PedidoServiceImpl;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private PedidoEventoRepository pedidoEventoRepository;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private OutboxService outboxService;
    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private Counter counter;
    @Mock
    private WorkflowProperties workflowProperties;
    @Mock
    private WorkflowProperties.Messages workflowMessages;
    @Mock
    private CamundaGatewayClient camundaGatewayClient;
    @Mock
    private OrchestrationProperties orchestrationProperties;
    @Mock
    private OrchestrationProperties.Keys orchestrationKeys;

    private PedidoServiceImpl pedidoService;

    @BeforeEach
    void setUp() {
        lenient().when(meterRegistry.counter(eq("pedido.transition.total"), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(counter);
        lenient().when(workflowProperties.getMessages()).thenReturn(workflowMessages);
        lenient().when(workflowMessages.getAprobadoPedido()).thenReturn("pedido-aprobado");
        lenient().when(workflowMessages.getPagadoPedido()).thenReturn("pedido-pagado");
        lenient().when(workflowMessages.getCanceladoPedido()).thenReturn("pedido-cancelado");
        lenient().when(orchestrationProperties.getKeys()).thenReturn(orchestrationKeys);
        lenient().when(orchestrationKeys.getProcessPedido()).thenReturn("PEDIDO_PROCESS");
        pedidoService = new PedidoServiceImpl(
                pedidoRepository,
                pedidoEventoRepository,
                idempotencyService,
                outboxService,
                meterRegistry,
                workflowProperties,
                camundaGatewayClient,
                orchestrationProperties
        );
    }

    @Test
    void iniciar_debeGuardarPedidoYRetornarKeysDeProceso() {
        CrearPedidoRequest request = new CrearPedidoRequest("Mateo", 100);
        when(pedidoRepository.save(any(PedidoEntity.class))).thenAnswer(invocation -> {
            PedidoEntity entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(10L);
            }
            return entity;
        });
        when(camundaGatewayClient.startProcess(any()))
                .thenReturn(new StartProcessGatewayResponse(123L, 456L, "proceso-pedido"));

        var response = pedidoService.iniciar(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.processInstanceKey()).isEqualTo(123L);
        assertThat(response.processDefinitionKey()).isEqualTo(456L);
        verify(camundaGatewayClient).startProcess(any());
    }

    @Test
    void aprobar_debeFallarSiNoEstaPendiente() {
        PedidoEntity pedido = buildPedido(1L, EstadoPedido.APROBADO);
        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.aprobar(1L, "k-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDIENTE");
    }

    @Test
    void pagar_debeRetornarRespuestaExistentePorIdempotencia() {
        CambioEstadoResponse existente = new CambioEstadoResponse(3L, "PAGADO");
        PedidoEntity pedido = buildPedido(3L, EstadoPedido.APROBADO);
        when(pedidoRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(pedido));
        when(idempotencyService.reserve("k-pay", "PAGAR_PEDIDO", new CambioEstadoResponse(3L, "PAGADO")))
                .thenReturn(new IdempotencyReservation(false, existente));

        CambioEstadoResponse result = pedidoService.pagar(3L, "k-pay");

        assertThat(result).isEqualTo(existente);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void historial_debeFallarSiPedidoNoExiste() {
        when(pedidoRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> pedidoService.historial(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void historial_debeRetornarEventosOrdenadosMapeados() {
        PedidoEntity pedido = buildPedido(10L, EstadoPedido.PAGADO);

        PedidoEventoEntity evento = new PedidoEventoEntity();
        evento.setId(7L);
        evento.setPedido(pedido);
        evento.setAccion("PAGAR_PEDIDO");
        evento.setEstadoAnterior(EstadoPedido.APROBADO);
        evento.setEstadoNuevo(EstadoPedido.PAGADO);
        evento.setFechaEvento(LocalDateTime.of(2026, 3, 5, 10, 0));

        when(pedidoRepository.existsById(10L)).thenReturn(true);
        when(pedidoEventoRepository.findByPedidoIdOrderByFechaEventoAsc(10L)).thenReturn(List.of(evento));

        List<PedidoEventoResponse> historial = pedidoService.historial(10L);

        assertThat(historial).hasSize(1);
        assertThat(historial.getFirst().accion()).isEqualTo("PAGAR_PEDIDO");
        assertThat(historial.getFirst().estadoAnterior()).isEqualTo("APROBADO");
        assertThat(historial.getFirst().estadoNuevo()).isEqualTo("PAGADO");
    }

    @Test
    void listar_debeMapearRespuestaPaginada() {
        PedidoEntity pedido = buildPedido(5L, EstadoPedido.PENDIENTE);
        pedido.setCliente("Mateo");
        pedido.setMonto(150);
        pedido.setProcessInstanceKey(100L);
        pedido.setProcessDefinitionKey(200L);
        pedido.setFechaCreacion(LocalDateTime.of(2026, 3, 5, 9, 0));
        pedido.setFechaActualizacion(LocalDateTime.of(2026, 3, 5, 9, 5));

        when(pedidoRepository.findAll(org.mockito.ArgumentMatchers.<Specification<PedidoEntity>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(pedido)));

        var result = pedidoService.listar("mat", EstadoPedido.PENDIENTE, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().id()).isEqualTo(5L);
        assertThat(result.getContent().getFirst().cliente()).isEqualTo("Mateo");
        assertThat(result.getContent().getFirst().fechaCreacion()).isNotNull();
    }

    @Test
    void cancelar_debeEncolarOutboxYGuardarIdempotencia() {
        PedidoEntity pedido = buildPedido(50L, EstadoPedido.PENDIENTE);
        when(pedidoRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(pedido));
        when(idempotencyService.reserve(eq("k-can"), eq("CANCELAR_PEDIDO"), any(CambioEstadoResponse.class)))
                .thenAnswer(invocation -> new IdempotencyReservation(true, invocation.getArgument(2)));

        CambioEstadoResponse result = pedidoService.cancelar(50L, "k-can");

        assertThat(result.estado()).isEqualTo("CANCELADO");
        verify(outboxService).enqueueWorkflowMessage(50L, "corr-50", "pedido-cancelado", "CANCELADO");
        verify(idempotencyService).reserve(eq("k-can"), eq("CANCELAR_PEDIDO"), any(CambioEstadoResponse.class));
    }

    private PedidoEntity buildPedido(Long id, EstadoPedido estado) {
        PedidoEntity pedido = new PedidoEntity();
        pedido.setId(id);
        pedido.setEstado(estado);
        pedido.setPedidoCorrelationKey("corr-" + id);
        return pedido;
    }
}
