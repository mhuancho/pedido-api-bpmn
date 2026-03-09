package bpmn.pedido.app.utils;

public class Constants {
    Constants(){}

    public static final String EVENT_TYPE = "eventType";
    public static final String PEDIDO = "PEDIDO";
    public static final String INICIAR_PEDIDO = "INICIAR_PEDIDO";
    public static final String APROBAR_PEDIDO = "APROBAR_PEDIDO";
    public static final String CANCELAR_PEDIDO = "CANCELAR_PEDIDO";
    public static final String PAGAR_PEDIDO = "PAGAR_PEDIDO";
    public static final String WORKFLOW_MESSAGE = "WORKFLOW_MESSAGE";
    public static final String WORKFLOW_START = "WORKFLOW_START";
    public static final String PEDIDO_ID = "pedidoId";
    public static final String PEDIDO_CORRELATION = "pedidoCorrelationKey";
    public static final String STATUS = "estado";
    public static final String CLIENTE = "cliente";
    public static final String PEDIDO_NOT_FOUND_ONLY = "Pedido no encontrado: ";
    public static final String MONTO = "monto";
    public static final String ACCION = "accion";
    public static final String PEDIDO_NOT_FOUND = "Pedido no encontrado para outbox: ";
    public static final String TIPO_EVENT_NOT_SUPPORT = "Tipo de evento outbox no soportado: ";
    public static final String APROBAR_PEDIDO_ONLY_PENDING = "Solo se puede aprobar un pedido en estado PENDIENTE";
    public static final String APROBAR_PEDIDO_ONLY_APR = "Solo se puede pagar un pedido en estado APROBADO";
    public static final String NOT_FINALIZE = "No se puede cancelar un pedido ya finalizado";
    public static final String OUTBOX_EVENTS_PROCESSED_TOTAL = "outbox.events.processed.total";
    public static final String OUTBOX_EVENTS_FAILED_TOTAL = "outbox.events.failed.total";
    public static final String OUTBOX_EVENTS_RETRY_TOTAL = "outbox.events.retry.total";
    public static final String OUTBOX_EVENTS_PENDING = "outbox.events.pending";
    public static final String PEDIDO_TRANSITION_TOTAL = "pedido.transition.total";

    public static final String REALM_ACCESS = "realm_access";
    public static final String REALM_ACCESS_ROLES = "roles";
    public static final String RESOURCE_ACCESS = "resource_access";
    public static final String ROLE_NAME = "ROLE_";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_OPERADOR = "OPERADOR";

    public static final String REQUEST_MATCHERS_ACTUATOR_HEALTH = "/actuator/health/**";
    public static final String REQUEST_MATCHERS_INFO = "/actuator/info";
    public static final String REQUEST_MATCHERS_ACTUATOR = "/actuator/**";
    public static final String REQUEST_MATCHERS_API_PEDIDO = "/api/pedidos/**";

    public static final String MSG_ERROR_VALIDATION = "Error de validacion";
    public static final String MSG_ERROR_NOT_AUTH = "No autenticado";
    public static final String MSG_ERROR_NOT_AUTHORIZE = "No autorizado";
    public static final String MSG_ERROR_INTERNAL_SERVER_ERROR = "Error interno del servidor";
    public static final String MSG_ERROR_ID_USED = "La clave de idempotencia ya fue usada en otra operacion";
}
