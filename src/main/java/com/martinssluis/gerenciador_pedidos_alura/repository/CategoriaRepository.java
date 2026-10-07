package com.martinssluis.gerenciador_pedidos_alura.repository;

import com.martinssluis.gerenciador_pedidos_alura.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long>{}
