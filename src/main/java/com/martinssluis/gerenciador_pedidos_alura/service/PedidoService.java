package com.martinssluis.gerenciador_pedidos_alura.service;

import com.martinssluis.gerenciador_pedidos_alura.dto.AdicionarItemRequest;
import com.martinssluis.gerenciador_pedidos_alura.dto.PedidoDetalheResponse;
import com.martinssluis.gerenciador_pedidos_alura.dto.PedidoResumoResponse;
import com.martinssluis.gerenciador_pedidos_alura.exception.RecursoNaoEncontradoException;
import com.martinssluis.gerenciador_pedidos_alura.model.Pedido;
import com.martinssluis.gerenciador_pedidos_alura.repository.ItemPedidoRepository;
import com.martinssluis.gerenciador_pedidos_alura.repository.PedidoPlsqlRepository;
import com.martinssluis.gerenciador_pedidos_alura.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * A regra de negocio de pedidos mora no PL/SQL (pkg_pedidos).
 * O service orquestra as chamadas e CONTROLA A TRANSACAO:
 * se qualquer chamada falhar, o @Transactional faz rollback de tudo.
 */
@Service
public class PedidoService {

    private final PedidoPlsqlRepository plsqlRepository;
    private final PedidoRepository pedidoRepository;
    private final ItemPedidoRepository itemPedidoRepository;

    public PedidoService(PedidoPlsqlRepository plsqlRepository,
                         PedidoRepository pedidoRepository,
                         ItemPedidoRepository itemPedidoRepository) {
        this.plsqlRepository = plsqlRepository;
        this.pedidoRepository = pedidoRepository;
        this.itemPedidoRepository = itemPedidoRepository;
    }

    @Transactional
    public PedidoDetalheResponse criar(String cliente) {
        Long pedidoId = plsqlRepository.criarPedido(cliente);
        return montarDetalhe(pedidoId);
    }

    @Transactional
    public PedidoDetalheResponse adicionarItem(Long pedidoId, AdicionarItemRequest request) {
        plsqlRepository.adicionarItem(pedidoId, request.produtoId(), request.quantidade());
        return montarDetalhe(pedidoId);
    }

    @Transactional
    public PedidoDetalheResponse finalizar(Long pedidoId) {
        pedidoRepository.finalizarPedido(pedidoId); // chamada via @Procedure
        return montarDetalhe(pedidoId);
    }

    @Transactional
    public PedidoDetalheResponse cancelar(Long pedidoId) {
        plsqlRepository.cancelarPedido(pedidoId);
        return montarDetalhe(pedidoId);
    }

    @Transactional(readOnly = true)
    public PedidoDetalheResponse buscar(Long pedidoId) {
        return montarDetalhe(pedidoId);
    }

    @Transactional(readOnly = true)
    public List<PedidoResumoResponse> listarPorCliente(String cliente) {
        return plsqlRepository.listarPedidosCliente(cliente);
    }

    private PedidoDetalheResponse montarDetalhe(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido " + pedidoId + " nao encontrado."));

        List<PedidoDetalheResponse.ItemResponse> itens = itemPedidoRepository.buscarPorPedido(pedidoId).stream()
                .map(item -> new PedidoDetalheResponse.ItemResponse(
                        item.getProduto().getId(),
                        item.getProduto().getNome(),
                        item.getQuantidade(),
                        item.getValorUnitario()))
                .toList();

        return new PedidoDetalheResponse(
                pedido.getId(),
                pedido.getCliente(),
                pedido.getData(),
                pedido.getStatus(),
                pedido.getValorTotal(),
                plsqlRepository.calcularTotal(pedidoId), // chamada da FUNCTION
                itens);
    }
}
