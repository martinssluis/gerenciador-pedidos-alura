package com.martinssluis.gerenciador_pedidos_alura.dto;

import java.math.BigDecimal;

public record ProdutoRequest(String nome, BigDecimal preco, Integer estoque, Long categoriaId) {
}
