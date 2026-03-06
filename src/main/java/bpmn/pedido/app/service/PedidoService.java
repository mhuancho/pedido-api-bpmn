package bpmn.pedido.app.service;


import bpmn.pedido.app.model.dto.CambioEstadoResponse;
import bpmn.pedido.app.model.dto.CrearPedidoRequest;
import bpmn.pedido.app.model.dto.PedidoEventoResponse;
import bpmn.pedido.app.model.dto.PedidoResponse;
import bpmn.pedido.app.model.enums.EstadoPedido;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PedidoService {
    PedidoResponse iniciar(CrearPedidoRequest request);
    CambioEstadoResponse aprobar(Long id, String idempotencyKey);
    CambioEstadoResponse pagar(Long id, String idempotencyKey);
    CambioEstadoResponse cancelar(Long id, String idempotencyKey);
    PedidoResponse obtener(Long id);
    Page<PedidoResponse> listar(String cliente, EstadoPedido estado, int page, int size);
    List<PedidoEventoResponse> historial(Long pedidoId);
}
