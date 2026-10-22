package com.martinssluis.gerenciador_pedidos_alura.principal;

import com.martinssluis.gerenciador_pedidos_alura.model.Categoria;
import com.martinssluis.gerenciador_pedidos_alura.model.Pedido;
import com.martinssluis.gerenciador_pedidos_alura.model.Produto;
import com.martinssluis.gerenciador_pedidos_alura.repository.CategoriaRepository;
import com.martinssluis.gerenciador_pedidos_alura.repository.PedidoRepository;
import com.martinssluis.gerenciador_pedidos_alura.repository.ProdutoRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class Principal {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final PedidoRepository pedidoRepository;

    public Principal(ProdutoRepository produtoRepository, CategoriaRepository categoriaRepository, PedidoRepository pedidoRepository) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.pedidoRepository = pedidoRepository;
    }

    public void salvarNoBanco(){
        Produto produto = new Produto("Iphone 17 Pro", 7799.00);
        Categoria categoria = new Categoria(1L, "Celulares");
        Pedido pedido = new Pedido(1L, LocalDate.now());

        produtoRepository.save(produto);
        categoriaRepository.save(categoria);
        pedidoRepository.save(pedido);
    }

    public void buscarProdutoPorNome(String nomeProduto){
        List<Produto> produtos = produtoRepository.findByNomeContainingIgnoreCase(nomeProduto);

        if(produtos.isEmpty()){
            System.out.println("Nenhum produto encontrado com: " + nomeProduto);
        }else {
            produtos.forEach(System.out::println);
        }
    }
}
