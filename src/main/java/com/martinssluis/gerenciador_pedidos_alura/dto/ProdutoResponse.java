package com.martinssluis.gerenciador_pedidos_alura.dto;

import com.martinssluis.gerenciador_pedidos_alura.model.Produto;

import java.math.BigDecimal;

public record ProdutoResponse(Long id, String nome, BigDecimal preco, Integer estoque, String categoria) {

    public static ProdutoResponse de(Produto produto) {
        return new ProdutoResponse(
                produto.getId(),
                produto.getNome(),
                produto.getPreco(),
                produto.getEstoque(),
                produto.getCategoria() == null ? null : produto.getCategoria().getNome());
    }
}
