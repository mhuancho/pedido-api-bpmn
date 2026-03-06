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
@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyRecordRepository repository;

    @Transactional(readOnly = true)
    @Override
    public void validateNoCrossReuse(String key, String accion, Long pedidoId) {
        repository.findByIdempotencyKey(key)
                .filter(record -> !record.getAccion().equals(accion) || !record.getPedidoId().equals(pedidoId))
                .ifPresent(record -> {
                    throw new IdempotencyConflictException("La clave de idempotencia ya fue usada en otra operacion");
                });
    }

    @Transactional
    @Override
    public IdempotencyReservation reserve(String key, String accion, CambioEstadoResponse response) {
        try {
            IdempotencyRecordEntity record = new IdempotencyRecordEntity();
            record.setIdempotencyKey(key);
            record.setAccion(accion);
            record.setPedidoId(response.id());
            record.setEstadoResultante(response.estado());
            repository.save(record);
            return new IdempotencyReservation(true, response);
        } catch (DataIntegrityViolationException ex) {
            return repository.findByIdempotencyKey(key)
                    .map(record -> {
                        boolean sameOperation = record.getAccion().equals(accion) && record.getPedidoId().equals(response.id());
                        if (!sameOperation) {
                            throw new IdempotencyConflictException("La clave de idempotencia ya fue usada en otra operacion");
                        }
                        CambioEstadoResponse existingResponse =
                                new CambioEstadoResponse(record.getPedidoId(), record.getEstadoResultante());
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
