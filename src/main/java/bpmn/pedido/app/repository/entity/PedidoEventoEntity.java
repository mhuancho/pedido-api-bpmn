package bpmn.pedido.app.repository.entity;

import bpmn.pedido.app.model.enums.EstadoPedido;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "pedido_evento")
@Getter
@Setter
public class PedidoEventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoEntity pedido;

    @Column(nullable = false)
    private String accion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior")
    private EstadoPedido estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false)
    private EstadoPedido estadoNuevo;

    @Column(nullable = false)
    private LocalDateTime fechaEvento;

    @PrePersist
    void prePersist() {
        fechaEvento = LocalDateTime.now();
    }
}
