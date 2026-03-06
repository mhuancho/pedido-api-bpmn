package bpmn.pedido.app.controller;

import bpmn.pedido.app.model.dto.CambioEstadoResponse;
import bpmn.pedido.app.model.dto.CrearPedidoRequest;
import bpmn.pedido.app.model.dto.PedidoEventoResponse;
import bpmn.pedido.app.model.dto.PedidoResponse;
import bpmn.pedido.app.model.enums.EstadoPedido;
import bpmn.pedido.app.service.PedidoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static bpmn.pedido.app.utils.Helpers.requireIdempotencyKey;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Validated
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    public PedidoResponse iniciar(@Valid @RequestBody CrearPedidoRequest request) {
        return pedidoService.iniciar(request);
    }

    @PostMapping("/{id}/aprobar")
    public CambioEstadoResponse aprobar(
            @PathVariable Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return pedidoService.aprobar(id, requireIdempotencyKey(idempotencyKey));
    }

    @PostMapping("/{id}/pagar")
    public CambioEstadoResponse pagar(
            @PathVariable Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return pedidoService.pagar(id, requireIdempotencyKey(idempotencyKey));
    }

    @PostMapping("/{id}/cancelar")
    public CambioEstadoResponse cancelar(
            @PathVariable Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return pedidoService.cancelar(id, requireIdempotencyKey(idempotencyKey));
    }

    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Long id) {
        return pedidoService.obtener(id);
    }

    @GetMapping
    public Page<PedidoResponse> listar(
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return pedidoService.listar(cliente, estado, page, size);
    }

    @GetMapping("/{id}/eventos")
    public List<PedidoEventoResponse> historial(@PathVariable Long id) {
        return pedidoService.historial(id);
    }


}
