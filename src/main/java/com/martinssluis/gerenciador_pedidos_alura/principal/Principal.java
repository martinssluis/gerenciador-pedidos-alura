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
        Categoria categoria = categoriaRepository.save(new Categoria(1L, "Celulares"));
        Produto produto = new Produto("Iphone 17 Pro", 7799.00, categoria);
        Pedido pedido = new Pedido(1L, LocalDate.now());

        produtoRepository.save(produto);
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

    public void findByCategoria(String categoriaNome) {
        List<Produto> produtos = produtoRepository.findByCategoriaNome(categoriaNome);
        if(produtos.isEmpty()){
            System.out.println("Nenhum produto encontrado com a categoria : " + categoriaNome);
        }else {
            produtos.forEach(System.out::println);
        }
    }

    public void findProdutoByPrecoGreaterThan(Double preco){
        List<Produto> produtos = produtoRepository.findByPrecoGreaterThan(preco);
        if (produtos.isEmpty()){
            System.out.println("Nenhum produto encontrado com preço maior que: R$"+preco);
        }else {
            produtos.forEach(System.out::println);
        }
    }
}
