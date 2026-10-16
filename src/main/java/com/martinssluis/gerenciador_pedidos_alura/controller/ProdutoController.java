package com.martinssluis.gerenciador_pedidos_alura.controller;

import com.martinssluis.gerenciador_pedidos_alura.dto.ProdutoRequest;
import com.martinssluis.gerenciador_pedidos_alura.dto.ProdutoResponse;
import com.martinssluis.gerenciador_pedidos_alura.service.ProdutoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    public List<ProdutoResponse> listar() {
        return produtoService.listar();
    }

    @GetMapping("/{id}")
    public ProdutoResponse buscar(@PathVariable Long id) {
        return produtoService.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoResponse criar(@RequestBody ProdutoRequest request) {
        return produtoService.criar(request);
    }
}
