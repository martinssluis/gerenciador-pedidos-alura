package com.martinssluis.gerenciador_pedidos_alura.repository;

import com.martinssluis.gerenciador_pedidos_alura.dto.PedidoResumoResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.List;
import java.util.Map;

/**
 * Jeito 2 de chamar PL/SQL: SimpleJdbcCall (Spring JDBC).
 * Mais controle: parametros OUT, FUNCTION com retorno e SYS_REFCURSOR.
 * E o mais comum em sistemas com muitas procedures.
 *
 * withCatalogName = nome do PACKAGE no Oracle.
 * withoutProcedureColumnMetaDataAccess = nao consulta o dicionario do banco;
 * os parametros sao declarados aqui, na mesma ordem da procedure.
 *
 * MapSqlParameterSource (e nao Map.of) porque aceita valor nulo: quem valida
 * "cliente obrigatorio" ou "quantidade invalida" e o PL/SQL.
 */
@Repository
public class PedidoPlsqlRepository {

    private static final String PACOTE = "PKG_PEDIDOS";

    private final SimpleJdbcCall criarPedido;
    private final SimpleJdbcCall adicionarItem;
    private final SimpleJdbcCall cancelarPedido;
    private final SimpleJdbcCall calcularTotal;
    private final SimpleJdbcCall listarPedidosCliente;

    public PedidoPlsqlRepository(JdbcTemplate jdbcTemplate) {
        this.criarPedido = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACOTE)
                .withProcedureName("CRIAR_PEDIDO")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_CLIENTE", Types.VARCHAR),
                        new SqlOutParameter("P_PEDIDO_ID", Types.NUMERIC));

        this.adicionarItem = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACOTE)
                .withProcedureName("ADICIONAR_ITEM")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_PEDIDO_ID", Types.NUMERIC),
                        new SqlParameter("P_PRODUTO_ID", Types.NUMERIC),
                        new SqlParameter("P_QUANTIDADE", Types.NUMERIC));

        this.cancelarPedido = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACOTE)
                .withProcedureName("CANCELAR_PEDIDO")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(new SqlParameter("P_PEDIDO_ID", Types.NUMERIC));

        // FUNCTION: o primeiro parametro declarado e o RETORNO.
        this.calcularTotal = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACOTE)
                .withFunctionName("CALCULAR_TOTAL")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlOutParameter("RETORNO", Types.NUMERIC),
                        new SqlParameter("P_PEDIDO_ID", Types.NUMERIC));

        // SYS_REFCURSOR: o Spring percorre o cursor e aplica o RowMapper em cada linha.
        this.listarPedidosCliente = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName(PACOTE)
                .withProcedureName("LISTAR_PEDIDOS_CLIENTE")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("P_CLIENTE", Types.VARCHAR),
                        new SqlOutParameter("P_RESULTADO", Types.REF_CURSOR,
                                (rs, linha) -> new PedidoResumoResponse(
                                        rs.getLong("ID"),
                                        rs.getString("CLIENTE"),
                                        rs.getDate("DATA_PEDIDO").toLocalDate(),
                                        rs.getString("STATUS"),
                                        rs.getBigDecimal("VALOR_TOTAL"),
                                        rs.getInt("QTD_ITENS"))));
    }

    public Long criarPedido(String cliente) {
        Map<String, Object> saida = criarPedido.execute(new MapSqlParameterSource("P_CLIENTE", cliente));
        return ((Number) saida.get("P_PEDIDO_ID")).longValue();
    }

    public void adicionarItem(Long pedidoId, Long produtoId, Integer quantidade) {
        adicionarItem.execute(new MapSqlParameterSource()
                .addValue("P_PEDIDO_ID", pedidoId)
                .addValue("P_PRODUTO_ID", produtoId)
                .addValue("P_QUANTIDADE", quantidade));
    }

    public void cancelarPedido(Long pedidoId) {
        cancelarPedido.execute(new MapSqlParameterSource("P_PEDIDO_ID", pedidoId));
    }

    public BigDecimal calcularTotal(Long pedidoId) {
        return calcularTotal.executeFunction(BigDecimal.class, new MapSqlParameterSource("P_PEDIDO_ID", pedidoId));
    }

    @SuppressWarnings("unchecked")
    public List<PedidoResumoResponse> listarPedidosCliente(String cliente) {
        Map<String, Object> saida = listarPedidosCliente.execute(new MapSqlParameterSource("P_CLIENTE", cliente));
        return (List<PedidoResumoResponse>) saida.get("P_RESULTADO");
    }
}
