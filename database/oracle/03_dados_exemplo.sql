-- =====================================================================
-- 03_dados_exemplo.sql
-- Dados iniciais para testar. Rode depois do 01 e do 02.
-- =====================================================================

INSERT INTO categorias (nome) VALUES ('Celulares');
INSERT INTO categorias (nome) VALUES ('Notebooks');
INSERT INTO categorias (nome) VALUES ('Acessorios');

-- O id da categoria e buscado pelo nome, porque o IDENTITY gera os ids sozinho.
INSERT INTO produtos (nome, valor, estoque, categoria_id)
VALUES ('Iphone 17 Pro', 7799.00, 10, (SELECT id FROM categorias WHERE nome = 'Celulares'));

INSERT INTO produtos (nome, valor, estoque, categoria_id)
VALUES ('Galaxy S26', 5499.90, 15, (SELECT id FROM categorias WHERE nome = 'Celulares'));

INSERT INTO produtos (nome, valor, estoque, categoria_id)
VALUES ('Notebook Dell Inspiron', 4299.00, 5, (SELECT id FROM categorias WHERE nome = 'Notebooks'));

INSERT INTO produtos (nome, valor, estoque, categoria_id)
VALUES ('Mouse sem fio', 89.90, 50, (SELECT id FROM categorias WHERE nome = 'Acessorios'));

INSERT INTO produtos (nome, valor, estoque, categoria_id)
VALUES ('Fone Bluetooth', 199.90, 2, (SELECT id FROM categorias WHERE nome = 'Acessorios'));

-- No Oracle nada fica salvo para outras sessoes sem COMMIT.
-- (O DBeaver vem com auto-commit ligado, mas e bom ter o habito.)
COMMIT;

SELECT p.id, p.nome, p.valor, p.estoque, c.nome AS categoria
  FROM produtos p
  JOIN categorias c ON c.id = p.categoria_id
 ORDER BY p.id;
