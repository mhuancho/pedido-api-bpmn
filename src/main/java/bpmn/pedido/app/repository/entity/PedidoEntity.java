package bpmn.pedido.app.repository.entity;

import bpmn.pedido.app.model.enums.EstadoPedido;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "pedido", indexes = {
        @Index(name = "idx_pedido_estado", columnList = "estado"),
        @Index(name = "idx_pedido_cliente", columnList = "cliente")
})
@Getter
@Setter
public class PedidoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cliente;

    @Column(nullable = false)
    private Integer monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado;

    @Column(name = "process_instance_key")
    private Long processInstanceKey;

    @Column(name = "process_definition_key")
    private Long processDefinitionKey;

    @Column(name = "pedido_correlation_key", nullable = false, unique = true)
    private String pedidoCorrelationKey;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void prePersist() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
