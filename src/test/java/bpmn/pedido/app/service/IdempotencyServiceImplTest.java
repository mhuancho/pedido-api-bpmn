package bpmn.pedido.app.service;

import bpmn.pedido.app.exception.IdempotencyConflictException;
import bpmn.pedido.app.model.IdempotencyReservation;
import bpmn.pedido.app.model.dto.CambioEstadoResponse;
import bpmn.pedido.app.repository.dao.IdempotencyRecordRepository;
import bpmn.pedido.app.repository.entity.IdempotencyRecordEntity;
import bpmn.pedido.app.service.impl.IdempotencyServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceImplTest {

    @Mock
    private IdempotencyRecordRepository repository;

    @InjectMocks
    private IdempotencyServiceImpl idempotencyService;

    @Test
    void reserve_debeRetornarExistingSiYaExisteMismaOperacion() {
        CambioEstadoResponse expected = new CambioEstadoResponse(10L, "APROBADO");
        IdempotencyRecordEntity existing = new IdempotencyRecordEntity();
        existing.setIdempotencyKey("k-1");
        existing.setAccion("APROBAR_PEDIDO");
        existing.setPedidoId(10L);
        existing.setEstadoResultante("APROBADO");

        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(repository.findByIdempotencyKey("k-1")).thenReturn(Optional.of(existing));

        IdempotencyReservation result = idempotencyService.reserve("k-1", "APROBAR_PEDIDO", expected);

        assertThat(result.shouldProcess()).isFalse();
        assertThat(result.response()).isEqualTo(expected);
    }

    @Test
    void reserve_debeFallarSiLaClaveSeUsoEnOtraOperacion() {
        CambioEstadoResponse expected = new CambioEstadoResponse(10L, "APROBADO");
        IdempotencyRecordEntity existing = new IdempotencyRecordEntity();
        existing.setIdempotencyKey("k-1");
        existing.setAccion("PAGAR_PEDIDO");
        existing.setPedidoId(10L);
        existing.setEstadoResultante("PAGADO");

        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        when(repository.findByIdempotencyKey("k-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> idempotencyService.reserve("k-1", "APROBAR_PEDIDO", expected))
                .isInstanceOf(IdempotencyConflictException.class);
    }
}
