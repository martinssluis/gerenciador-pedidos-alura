package com.martinssluis.gerenciador_pedidos_alura.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PedidoDetalheResponse(
        Long id,
        String cliente,
        LocalDate data,
        String status,
        BigDecimal valorTotal,
        BigDecimal totalCalculadoPelaFunction,
        List<ItemResponse> itens) {

    public record ItemResponse(Long produtoId, String produto, Integer quantidade, BigDecimal valorUnitario) {
    }
}
