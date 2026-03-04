package bpmn.pedido.app.controller;

import bpmn.pedido.app.dto.CambioEstadoResponse;
import bpmn.pedido.app.dto.CrearPedidoRequest;
import bpmn.pedido.app.dto.PedidoResponse;
import bpmn.pedido.app.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    public PedidoResponse iniciar(@Valid @RequestBody CrearPedidoRequest request) {
        return pedidoService.iniciar(request);
    }

    @PostMapping("/{id}/aprobar")
    public CambioEstadoResponse aprobar(@PathVariable Long id) {
        return pedidoService.aprobar(id);
    }

    @PostMapping("/{id}/pagar")
    public CambioEstadoResponse pagar(@PathVariable Long id) {
        return pedidoService.pagar(id);
    }

    @PostMapping("/{id}/cancelar")
    public CambioEstadoResponse cancelar(@PathVariable Long id) {
        return pedidoService.cancelar(id);
    }

    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable Long id) {
        return pedidoService.obtener(id);
    }
}