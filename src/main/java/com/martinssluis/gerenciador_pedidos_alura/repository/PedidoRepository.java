package com.martinssluis.gerenciador_pedidos_alura.repository;

import com.martinssluis.gerenciador_pedidos_alura.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {}
