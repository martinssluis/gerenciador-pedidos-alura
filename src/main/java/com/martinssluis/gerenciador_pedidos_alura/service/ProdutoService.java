package com.martinssluis.gerenciador_pedidos_alura.service;

import com.martinssluis.gerenciador_pedidos_alura.dto.ProdutoRequest;
import com.martinssluis.gerenciador_pedidos_alura.dto.ProdutoResponse;
import com.martinssluis.gerenciador_pedidos_alura.exception.RecursoNaoEncontradoException;
import com.martinssluis.gerenciador_pedidos_alura.model.Categoria;
import com.martinssluis.gerenciador_pedidos_alura.model.Produto;
import com.martinssluis.gerenciador_pedidos_alura.repository.CategoriaRepository;
import com.martinssluis.gerenciador_pedidos_alura.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Cadastro simples de produtos: aqui o JPA faz tudo, sem PL/SQL. */
@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProdutoService(ProdutoRepository produtoRepository, CategoriaRepository categoriaRepository) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listar() {
        return produtoRepository.findAll().stream().map(ProdutoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscar(Long id) {
        return produtoRepository.findById(id)
                .map(ProdutoResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto " + id + " nao encontrado."));
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest request) {
        Categoria categoria = null;
        if (request.categoriaId() != null) {
            categoria = categoriaRepository.findById(request.categoriaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Categoria " + request.categoriaId() + " nao encontrada."));
        }
        Produto produto = new Produto(request.nome(), request.preco(), request.estoque(), categoria);
        return ProdutoResponse.de(produtoRepository.saveAndFlush(produto));
    }
}
