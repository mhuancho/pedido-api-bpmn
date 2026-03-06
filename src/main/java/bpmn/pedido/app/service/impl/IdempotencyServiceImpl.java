package bpmn.pedido.app.service.impl;

import bpmn.pedido.app.repository.dao.IdempotencyRecordRepository;
import bpmn.pedido.app.model.dto.CambioEstadoResponse;
import bpmn.pedido.app.repository.entity.IdempotencyRecordEntity;
import bpmn.pedido.app.exception.IdempotencyConflictException;
import bpmn.pedido.app.model.IdempotencyReservation;
import bpmn.pedido.app.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static bpmn.pedido.app.utils.Constants.MSG_ERROR_ID_USED;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyRecordRepository repository;

    @Transactional(readOnly = true)
    @Override
    public void validateNoCrossReuse(String key, String accion, Long pedidoId) {
        repository.findByIdempotencyKey(key)
                .filter(idempotencyRecord -> !idempotencyRecord.getAccion().equals(accion) || !idempotencyRecord.getPedidoId().equals(pedidoId))
                .ifPresent(recordPresent -> {
                    throw new IdempotencyConflictException(MSG_ERROR_ID_USED);
                });
    }

    @Transactional
    @Override
    public IdempotencyReservation reserve(String key, String accion, CambioEstadoResponse response) {
        try {
            IdempotencyRecordEntity recordReserve = new IdempotencyRecordEntity();
            recordReserve.setIdempotencyKey(key);
            recordReserve.setAccion(accion);
            recordReserve.setPedidoId(response.id());
            recordReserve.setEstadoResultante(response.estado());
            repository.save(recordReserve);
            return new IdempotencyReservation(true, response);
        } catch (DataIntegrityViolationException ex) {
            return repository.findByIdempotencyKey(key)
                    .map(recordMap -> {
                        boolean sameOperation = recordMap.getAccion().equals(accion) && recordMap.getPedidoId().equals(response.id());
                        if (!sameOperation) {
                            throw new IdempotencyConflictException(MSG_ERROR_ID_USED);
                        }
                        CambioEstadoResponse existingResponse =
                                new CambioEstadoResponse(recordMap.getPedidoId(), recordMap.getEstadoResultante());
                        return new IdempotencyReservation(false, existingResponse);
                    })
                    .orElseThrow(() -> ex);
        }
    }

    @Value("${app.idempotency.retention-hours:168}")
    private long retentionHours;

    @Transactional
    @Override
    public int cleanupExpired() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(retentionHours);
        return (int) repository.deleteByCreatedAtBefore(cutoff);
    }
}
