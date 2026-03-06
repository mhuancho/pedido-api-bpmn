package bpmn.pedido.app.repository.dao;

import bpmn.pedido.app.repository.entity.OutboxEventEntity;
import bpmn.pedido.app.model.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

import static bpmn.pedido.app.repository.constants.Constantes.PENDING_GAUGE;
import static bpmn.pedido.app.repository.constants.Constantes.UPDATE_STATUS_FOR_ID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {
    @Query(value = PENDING_GAUGE, nativeQuery = true)
    List<Long> findIdsToClaim(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Modifying
    @Query(UPDATE_STATUS_FOR_ID)
    void updateStatusForIds(@Param("ids") List<Long> ids,
                           @Param("expectedStatus") OutboxStatus expectedStatus,
                           @Param("newStatus") OutboxStatus newStatus);

    List<OutboxEventEntity> findByIdIn(List<Long> ids);

    long countByStatus(OutboxStatus status);

    @Modifying
    long deleteByStatusAndUpdatedAtBefore(OutboxStatus status, LocalDateTime cutoff);
}
