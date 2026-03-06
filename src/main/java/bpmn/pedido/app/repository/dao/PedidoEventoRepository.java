package bpmn.pedido.app.repository.dao;

import bpmn.pedido.app.repository.entity.PedidoEventoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoEventoRepository extends JpaRepository<PedidoEventoEntity, Long> {
    List<PedidoEventoEntity> findByPedidoIdOrderByFechaEventoAsc(Long pedidoId);
}
