package com.martinssluis.gerenciador_pedidos_alura.repository;

import com.martinssluis.gerenciador_pedidos_alura.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /**
     * Jeito 1 de chamar PL/SQL: @Procedure do Spring Data JPA.
     * Gera algo como: {call PKG_PEDIDOS.FINALIZAR_PEDIDO(?)}
     * Simples para procedures so com parametros de entrada.
     */
    @Procedure(procedureName = "PKG_PEDIDOS.FINALIZAR_PEDIDO")
    void finalizarPedido(Long pedidoId);
}
