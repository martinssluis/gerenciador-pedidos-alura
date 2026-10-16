package com.martinssluis.gerenciador_pedidos_alura.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pedidos sao CRIADOS e ALTERADOS pelo package PL/SQL (pkg_pedidos).
 * O JPA aqui e usado so para LEITURA: @Immutable impede o Hibernate de fazer INSERT/UPDATE.
 */
@Entity
@Immutable
@Table(name = "pedidos")
public class Pedido {

    @Id
    private Long id;

    private String cliente;

    @Column(name = "data_pedido")
    private LocalDate data;

    private String status;

    @Column(name = "valor_total")
    private BigDecimal valorTotal;

    public Pedido() {}

    public Long getId() {
        return id;
    }

    public String getCliente() {
        return cliente;
    }

    public LocalDate getData() {
        return data;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }
}
