package com.martinssluis.gerenciador_pedidos_alura.repository;

import com.martinssluis.gerenciador_pedidos_alura.model.ItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    // JOIN FETCH traz o produto junto, numa query so (evita o problema N+1).
    @Query("select i from ItemPedido i join fetch i.produto where i.pedidoId = :pedidoId order by i.id")
    List<ItemPedido> buscarPorPedido(@Param("pedidoId") Long pedidoId);
}
