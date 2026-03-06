package bpmn.pedido.app.repository.constants;

public class Constantes {
    Constantes(){}
    public static final String PENDING_GAUGE =
            """
            SELECT id
            FROM outbox_event
            WHERE status = 'PENDING'
              AND next_attempt_at <= :now
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """
            ;

    public static final String UPDATE_STATUS_FOR_ID = """
            UPDATE OutboxEventEntity e
               SET e.status = :newStatus
             WHERE e.id IN :ids
               AND e.status = :expectedStatus
            """
            ;
}
