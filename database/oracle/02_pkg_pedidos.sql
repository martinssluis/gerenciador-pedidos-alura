-- =====================================================================
-- 02_pkg_pedidos.sql
-- Package PL/SQL com a regra de negocio de pedidos.
--
-- Um PACKAGE tem duas partes:
--   * SPEC (especificacao): o "contrato publico", o que pode ser chamado de fora.
--     Parecido com uma interface Java.
--   * BODY (corpo): a implementacao. Pode ter rotinas privadas que nao
--     aparecem na spec (aqui: registrar_log e travar_pedido_aberto).
--
-- Regra importante deste projeto: as procedures NAO fazem COMMIT.
-- Quem controla a transacao e o Java (@Transactional no service).
-- Assim, se algo falhar no meio, o Spring faz rollback de tudo junto.
-- =====================================================================

CREATE OR REPLACE PACKAGE pkg_pedidos AS

    -- Codigos de erro do negocio. RAISE_APPLICATION_ERROR aceita de -20000 a -20999.
    -- O Java le esses codigos para devolver o status HTTP certo.
    c_erro_pedido_nao_encontrado  CONSTANT PLS_INTEGER := -20001;
    c_erro_produto_nao_encontrado CONSTANT PLS_INTEGER := -20002;
    c_erro_estoque_insuficiente   CONSTANT PLS_INTEGER := -20003;
    c_erro_pedido_fechado         CONSTANT PLS_INTEGER := -20004;
    c_erro_dado_invalido          CONSTANT PLS_INTEGER := -20005;
    c_erro_pedido_vazio           CONSTANT PLS_INTEGER := -20006;

    -- Cria um pedido ABERTO e devolve o id gerado (parametro OUT).
    PROCEDURE criar_pedido (
        p_cliente   IN  VARCHAR2,
        p_pedido_id OUT NUMBER
    );

    -- Adiciona um item: valida pedido e produto, baixa estoque e recalcula o total.
    PROCEDURE adicionar_item (
        p_pedido_id  IN NUMBER,
        p_produto_id IN NUMBER,
        p_quantidade IN NUMBER
    );

    -- Fecha o pedido. Nao deixa finalizar pedido sem itens.
    PROCEDURE finalizar_pedido (
        p_pedido_id IN NUMBER
    );

    -- Cancela o pedido e devolve os itens ao estoque.
    PROCEDURE cancelar_pedido (
        p_pedido_id IN NUMBER
    );

    -- FUNCTION: sempre retorna um valor. Pode ser usada dentro de um SELECT.
    FUNCTION calcular_total (
        p_pedido_id IN NUMBER
    ) RETURN NUMBER;

    -- Devolve um cursor (SYS_REFCURSOR) com os pedidos do cliente.
    -- E assim que sistemas legados costumam "retornar uma lista" de uma procedure.
    PROCEDURE listar_pedidos_cliente (
        p_cliente   IN  VARCHAR2,
        p_resultado OUT SYS_REFCURSOR
    );

END pkg_pedidos;
/


CREATE OR REPLACE PACKAGE BODY pkg_pedidos AS

    -- -----------------------------------------------------------------
    -- PRIVADA: grava log numa transacao autonoma.
    -- PRAGMA AUTONOMOUS_TRANSACTION = esta rotina tem a PROPRIA transacao.
    -- O COMMIT daqui so confirma o log, e o log sobrevive mesmo se a
    -- operacao principal der ROLLBACK. Uso tipico: auditoria e log de erro.
    -- -----------------------------------------------------------------
    PROCEDURE registrar_log (
        p_pedido_id IN NUMBER,
        p_acao      IN VARCHAR2,
        p_mensagem  IN VARCHAR2
    ) IS
        PRAGMA AUTONOMOUS_TRANSACTION;
    BEGIN
        INSERT INTO log_pedidos (pedido_id, acao, mensagem)
        VALUES (p_pedido_id, p_acao, SUBSTR(p_mensagem, 1, 4000));
        COMMIT;
    END registrar_log;

    -- -----------------------------------------------------------------
    -- PRIVADA: confere se o pedido existe e esta ABERTO.
    -- FOR UPDATE trava a linha do pedido ate o fim da transacao, para
    -- duas requisicoes simultaneas nao alterarem o mesmo pedido juntas.
    -- -----------------------------------------------------------------
    PROCEDURE travar_pedido_aberto (
        p_pedido_id IN NUMBER
    ) IS
        v_status pedidos.status%TYPE;   -- %TYPE: herda o tipo da coluna
    BEGIN
        SELECT status
          INTO v_status
          FROM pedidos
         WHERE id = p_pedido_id
           FOR UPDATE;

        IF v_status <> 'ABERTO' THEN
            RAISE_APPLICATION_ERROR(
                c_erro_pedido_fechado,
                'Pedido ' || p_pedido_id || ' esta ' || v_status || ' e nao pode ser alterado.'
            );
        END IF;
    EXCEPTION
        -- SELECT ... INTO que nao acha nenhuma linha dispara NO_DATA_FOUND
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(
                c_erro_pedido_nao_encontrado,
                'Pedido ' || p_pedido_id || ' nao encontrado.'
            );
    END travar_pedido_aberto;

    -- -----------------------------------------------------------------
    FUNCTION calcular_total (
        p_pedido_id IN NUMBER
    ) RETURN NUMBER IS
        v_total NUMBER(12,2);
    BEGIN
        -- SUM nunca gera NO_DATA_FOUND; sem itens ele retorna NULL, por isso o NVL.
        SELECT NVL(SUM(quantidade * valor_unitario), 0)
          INTO v_total
          FROM itens_pedido
         WHERE pedido_id = p_pedido_id;

        RETURN v_total;
    END calcular_total;

    -- -----------------------------------------------------------------
    PROCEDURE criar_pedido (
        p_cliente   IN  VARCHAR2,
        p_pedido_id OUT NUMBER
    ) IS
    BEGIN
        IF TRIM(p_cliente) IS NULL THEN
            RAISE_APPLICATION_ERROR(c_erro_dado_invalido, 'O nome do cliente e obrigatorio.');
        END IF;

        -- RETURNING ... INTO devolve o id gerado pelo IDENTITY sem outro SELECT
        INSERT INTO pedidos (cliente)
        VALUES (TRIM(p_cliente))
        RETURNING id INTO p_pedido_id;

        registrar_log(p_pedido_id, 'CRIADO', 'Pedido criado para ' || TRIM(p_cliente));
    END criar_pedido;

    -- -----------------------------------------------------------------
    PROCEDURE adicionar_item (
        p_pedido_id  IN NUMBER,
        p_produto_id IN NUMBER,
        p_quantidade IN NUMBER
    ) IS
        v_valor   produtos.valor%TYPE;
        v_estoque produtos.estoque%TYPE;
        v_total   pedidos.valor_total%TYPE;
    BEGIN
        IF p_quantidade IS NULL OR p_quantidade <= 0 THEN
            RAISE_APPLICATION_ERROR(c_erro_dado_invalido, 'A quantidade deve ser maior que zero.');
        END IF;

        travar_pedido_aberto(p_pedido_id);

        -- Bloco aninhado: o EXCEPTION abaixo so vale para este SELECT.
        BEGIN
            SELECT valor, estoque
              INTO v_valor, v_estoque
              FROM produtos
             WHERE id = p_produto_id
               FOR UPDATE;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(
                    c_erro_produto_nao_encontrado,
                    'Produto ' || p_produto_id || ' nao encontrado.'
                );
        END;

        IF v_estoque < p_quantidade THEN
            -- O log fica gravado mesmo com o erro logo abaixo (transacao autonoma).
            registrar_log(
                p_pedido_id,
                'ESTOQUE_INSUFICIENTE',
                'Produto ' || p_produto_id || ': pedido ' || p_quantidade || ', disponivel ' || v_estoque
            );
            RAISE_APPLICATION_ERROR(
                c_erro_estoque_insuficiente,
                'Estoque insuficiente para o produto ' || p_produto_id || '. Disponivel: ' || v_estoque || '.'
            );
        END IF;

        UPDATE produtos
           SET estoque = estoque - p_quantidade
         WHERE id = p_produto_id;

        -- Grava o preco atual: se o produto mudar de preco depois, o pedido nao muda.
        INSERT INTO itens_pedido (pedido_id, produto_id, quantidade, valor_unitario)
        VALUES (p_pedido_id, p_produto_id, p_quantidade, v_valor);

        v_total := calcular_total(p_pedido_id);

        UPDATE pedidos
           SET valor_total = v_total
         WHERE id = p_pedido_id;
    END adicionar_item;

    -- -----------------------------------------------------------------
    PROCEDURE finalizar_pedido (
        p_pedido_id IN NUMBER
    ) IS
        v_qtd_itens NUMBER;
    BEGIN
        travar_pedido_aberto(p_pedido_id);

        SELECT COUNT(*)
          INTO v_qtd_itens
          FROM itens_pedido
         WHERE pedido_id = p_pedido_id;

        IF v_qtd_itens = 0 THEN
            RAISE_APPLICATION_ERROR(
                c_erro_pedido_vazio,
                'Pedido ' || p_pedido_id || ' nao tem itens e nao pode ser finalizado.'
            );
        END IF;

        UPDATE pedidos
           SET status      = 'FINALIZADO',
               valor_total = calcular_total(p_pedido_id)
         WHERE id = p_pedido_id;

        registrar_log(p_pedido_id, 'FINALIZADO', 'Pedido finalizado com ' || v_qtd_itens || ' item(ns).');
    END finalizar_pedido;

    -- -----------------------------------------------------------------
    PROCEDURE cancelar_pedido (
        p_pedido_id IN NUMBER
    ) IS
        v_status pedidos.status%TYPE;
    BEGIN
        BEGIN
            SELECT status
              INTO v_status
              FROM pedidos
             WHERE id = p_pedido_id
               FOR UPDATE;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RAISE_APPLICATION_ERROR(
                    c_erro_pedido_nao_encontrado,
                    'Pedido ' || p_pedido_id || ' nao encontrado.'
                );
        END;

        IF v_status = 'CANCELADO' THEN
            RAISE_APPLICATION_ERROR(c_erro_pedido_fechado, 'Pedido ' || p_pedido_id || ' ja esta cancelado.');
        END IF;

        -- Cursor FOR LOOP: percorre os itens e devolve cada um ao estoque.
        FOR item IN (SELECT produto_id, quantidade
                       FROM itens_pedido
                      WHERE pedido_id = p_pedido_id) LOOP
            UPDATE produtos
               SET estoque = estoque + item.quantidade
             WHERE id = item.produto_id;
        END LOOP;

        UPDATE pedidos
           SET status = 'CANCELADO'
         WHERE id = p_pedido_id;

        registrar_log(p_pedido_id, 'CANCELADO', 'Pedido cancelado. Status anterior: ' || v_status);
    END cancelar_pedido;

    -- -----------------------------------------------------------------
    PROCEDURE listar_pedidos_cliente (
        p_cliente   IN  VARCHAR2,
        p_resultado OUT SYS_REFCURSOR
    ) IS
    BEGIN
        OPEN p_resultado FOR
            SELECT p.id,
                   p.cliente,
                   p.data_pedido,
                   p.status,
                   p.valor_total,
                   (SELECT COUNT(*) FROM itens_pedido i WHERE i.pedido_id = p.id) AS qtd_itens
              FROM pedidos p
             WHERE UPPER(p.cliente) LIKE '%' || UPPER(TRIM(p_cliente)) || '%'
             ORDER BY p.data_pedido DESC, p.id DESC;
    END listar_pedidos_cliente;

END pkg_pedidos;
/

-- Confira se compilou: os dois devem aparecer como VALID.
-- Se aparecer INVALID, rode: SELECT * FROM user_errors WHERE name = 'PKG_PEDIDOS';
SELECT object_name, object_type, status
  FROM user_objects
 WHERE object_name = 'PKG_PEDIDOS';
