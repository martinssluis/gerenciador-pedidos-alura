package com.martinssluis.gerenciador_pedidos_alura.service;

import com.martinssluis.gerenciador_pedidos_alura.model.Categoria;
import com.martinssluis.gerenciador_pedidos_alura.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<Categoria> listar() {
        return categoriaRepository.findAll();
    }

    @Transactional
    public Categoria criar(String nome) {
        return categoriaRepository.saveAndFlush(new Categoria(nome));
    }
}
