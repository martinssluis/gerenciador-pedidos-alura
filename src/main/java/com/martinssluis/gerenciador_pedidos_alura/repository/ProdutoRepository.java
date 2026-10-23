package com.martinssluis.gerenciador_pedidos_alura.repository;

import com.martinssluis.gerenciador_pedidos_alura.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByNomeContainingIgnoreCase(String nomeProduto);

    List<Produto> findByCategoriaNome(String categoria);

    List<Produto> findByPrecoGreaterThan(Double preco);
}
