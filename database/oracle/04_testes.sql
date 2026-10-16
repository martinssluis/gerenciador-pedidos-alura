-- =====================================================================
-- 04_testes.sql
-- Exercicios para rodar no DBeaver, UM BLOCO POR VEZ.
-- Selecione o bloco (do DECLARE ate a barra /) e execute com Ctrl+Enter.
--
-- Para ver as mensagens do DBMS_OUTPUT.PUT_LINE no DBeaver:
-- ative o painel "Output" (Ctrl+Shift+O) antes de executar.
-- =====================================================================


-- ---------------------------------------------------------------------
-- TESTE 1: fluxo feliz. Cria pedido, adiciona itens, finaliza.
-- ---------------------------------------------------------------------
DECLARE
    v_pedido_id  NUMBER;
    v_iphone_id  NUMBER;
    v_mouse_id   NUMBER;
BEGIN
    SELECT id INTO v_iphone_id FROM produtos WHERE nome = 'Iphone 17 Pro';
    SELECT id INTO v_mouse_id  FROM produtos WHERE nome = 'Mouse sem fio';

    pkg_pedidos.criar_pedido(p_cliente => 'Luis Martins', p_pedido_id => v_pedido_id);
    DBMS_OUTPUT.PUT_LINE('Pedido criado: ' || v_pedido_id);

    pkg_pedidos.adicionar_item(v_pedido_id, v_iphone_id, 1);
    pkg_pedidos.adicionar_item(v_pedido_id, v_mouse_id, 2);
    DBMS_OUTPUT.PUT_LINE('Total: ' || pkg_pedidos.calcular_total(v_pedido_id));

    pkg_pedidos.finalizar_pedido(v_pedido_id);
    DBMS_OUTPUT.PUT_LINE('Pedido finalizado.');

    COMMIT;
END;
/

-- Confira o resultado nas tabelas
SELECT * FROM pedidos ORDER BY id;
SELECT * FROM itens_pedido ORDER BY id;
SELECT id, nome, estoque FROM produtos ORDER BY id;


-- ---------------------------------------------------------------------
-- TESTE 2: erro de negocio + ROLLBACK + log autonomo.
-- O Fone Bluetooth tem so 2 unidades. Vamos pedir 5.
-- Repare: o pedido e desfeito pelo ROLLBACK, mas o LOG continua la.
-- ---------------------------------------------------------------------
DECLARE
    v_pedido_id NUMBER;
    v_fone_id   NUMBER;
BEGIN
    SELECT id INTO v_fone_id FROM produtos WHERE nome = 'Fone Bluetooth';

    pkg_pedidos.criar_pedido('Cliente Teste Erro', v_pedido_id);
    pkg_pedidos.adicionar_item(v_pedido_id, v_fone_id, 5);

    COMMIT;
EXCEPTION
    WHEN OTHERS THEN
        -- SQLCODE = codigo do erro (-20003); SQLERRM = mensagem
        DBMS_OUTPUT.PUT_LINE('Erro ' || SQLCODE || ': ' || SQLERRM);
        ROLLBACK;
END;
/

-- O pedido 'Cliente Teste Erro' NAO existe (rollback)...
SELECT * FROM pedidos WHERE cliente = 'Cliente Teste Erro';
-- ...mas o log de CRIADO e de ESTOQUE_INSUFICIENTE existe (transacao autonoma).
SELECT * FROM log_pedidos ORDER BY id;


-- ---------------------------------------------------------------------
-- TESTE 3: FUNCTION usada direto num SELECT (procedure nao pode isso).
-- ---------------------------------------------------------------------
SELECT id,
       cliente,
       status,
       pkg_pedidos.calcular_total(id) AS total_calculado
  FROM pedidos
 ORDER BY id;


-- ---------------------------------------------------------------------
-- TESTE 4: lendo um SYS_REFCURSOR devolvido pela procedure.
-- ---------------------------------------------------------------------
DECLARE
    v_cursor     SYS_REFCURSOR;
    v_id         pedidos.id%TYPE;
    v_cliente    pedidos.cliente%TYPE;
    v_data       pedidos.data_pedido%TYPE;
    v_status     pedidos.status%TYPE;
    v_total      pedidos.valor_total%TYPE;
    v_qtd_itens  NUMBER;
BEGIN
    pkg_pedidos.listar_pedidos_cliente('luis', v_cursor);

    LOOP
        FETCH v_cursor INTO v_id, v_cliente, v_data, v_status, v_total, v_qtd_itens;
        EXIT WHEN v_cursor%NOTFOUND;
        DBMS_OUTPUT.PUT_LINE(v_id || ' | ' || v_cliente || ' | ' || v_status || ' | R$ ' || v_total || ' | itens: ' || v_qtd_itens);
    END LOOP;

    CLOSE v_cursor;
END;
/


-- ---------------------------------------------------------------------
-- TESTE 5: cancelar devolve o estoque.
-- Anote o estoque do Iphone antes e depois.
-- ---------------------------------------------------------------------
SELECT nome, estoque FROM produtos WHERE nome = 'Iphone 17 Pro';

DECLARE
    v_pedido_id NUMBER;
BEGIN
    SELECT MAX(id) INTO v_pedido_id FROM pedidos WHERE cliente = 'Luis Martins';
    pkg_pedidos.cancelar_pedido(v_pedido_id);
    DBMS_OUTPUT.PUT_LINE('Pedido ' || v_pedido_id || ' cancelado.');
    COMMIT;
END;
/

SELECT nome, estoque FROM produtos WHERE nome = 'Iphone 17 Pro';


-- ---------------------------------------------------------------------
-- TESTE 6: tente sozinho. Cada linha deve dar um erro diferente.
-- Descubra qual codigo (-2000x) cada uma gera.
-- ---------------------------------------------------------------------
-- BEGIN pkg_pedidos.adicionar_item(99999, 1, 1); END;
-- /
-- BEGIN pkg_pedidos.cancelar_pedido((SELECT MAX(id) FROM pedidos)); END;   -- dica: isso nao compila, por que?
-- /
-- DECLARE v_id NUMBER; BEGIN pkg_pedidos.criar_pedido('Vazio', v_id); pkg_pedidos.finalizar_pedido(v_id); END;
-- /
