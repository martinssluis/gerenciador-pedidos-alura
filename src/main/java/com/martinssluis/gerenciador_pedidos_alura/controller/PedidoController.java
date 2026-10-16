package com.martinssluis.gerenciador_pedidos_alura.controller;

import com.martinssluis.gerenciador_pedidos_alura.dto.AdicionarItemRequest;
import com.martinssluis.gerenciador_pedidos_alura.dto.CriarPedidoRequest;
import com.martinssluis.gerenciador_pedidos_alura.dto.PedidoDetalheResponse;
import com.martinssluis.gerenciador_pedidos_alura.dto.PedidoResumoResponse;
import com.martinssluis.gerenciador_pedidos_alura.service.PedidoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoDetalheResponse criar(@RequestBody CriarPedidoRequest request) {
        return pedidoService.criar(request.cliente());
    }

    @PostMapping("/{id}/itens")
    public PedidoDetalheResponse adicionarItem(@PathVariable Long id, @RequestBody AdicionarItemRequest request) {
        return pedidoService.adicionarItem(id, request);
    }

    @PostMapping("/{id}/finalizar")
    public PedidoDetalheResponse finalizar(@PathVariable Long id) {
        return pedidoService.finalizar(id);
    }

    @PostMapping("/{id}/cancelar")
    public PedidoDetalheResponse cancelar(@PathVariable Long id) {
        return pedidoService.cancelar(id);
    }

    @GetMapping("/{id}")
    public PedidoDetalheResponse buscar(@PathVariable Long id) {
        return pedidoService.buscar(id);
    }

    // GET /pedidos?cliente=luis  -> usa a procedure que devolve SYS_REFCURSOR
    @GetMapping
    public List<PedidoResumoResponse> listarPorCliente(@RequestParam(defaultValue = "") String cliente) {
        return pedidoService.listarPorCliente(cliente);
    }
}
