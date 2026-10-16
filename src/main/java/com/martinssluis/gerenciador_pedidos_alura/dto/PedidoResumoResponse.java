package com.martinssluis.gerenciador_pedidos_alura.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Uma linha do SYS_REFCURSOR devolvido por PKG_PEDIDOS.LISTAR_PEDIDOS_CLIENTE. */
public record PedidoResumoResponse(
        Long id,
        String cliente,
        LocalDate data,
        String status,
        BigDecimal valorTotal,
        Integer quantidadeItens) {
}
