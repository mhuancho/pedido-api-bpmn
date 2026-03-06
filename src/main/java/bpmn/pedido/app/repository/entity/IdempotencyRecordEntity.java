package bpmn.pedido.app.repository.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_record", uniqueConstraints = {
        @UniqueConstraint(name = "uk_idempotency_key", columnNames = {"idempotency_key"}),
        @UniqueConstraint(name = "uk_idempotency_key_action_pedido", columnNames = {"idempotency_key", "accion", "pedido_id"})
})
@Getter
@Setter
public class IdempotencyRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String accion;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(name = "estado_resultante", nullable = false, length = 32)
    private String estadoResultante;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
