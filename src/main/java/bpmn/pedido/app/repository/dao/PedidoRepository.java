package bpmn.pedido.app.repository.dao;


import bpmn.pedido.app.repository.entity.PedidoEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PedidoRepository extends JpaRepository<PedidoEntity, Long>, JpaSpecificationExecutor<PedidoEntity> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PedidoEntity p where p.id = :id")
    Optional<PedidoEntity> findByIdForUpdate(Long id);
}
