package bpmn.pedido.app.repository.dao;

import bpmn.pedido.app.repository.entity.IdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, Long> {

    Optional<IdempotencyRecordEntity> findByIdempotencyKey(String idempotencyKey);

    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
