# Gerenciador de Pedidos — versão Oracle + PL/SQL

Branch de estudo para praticar **Java + Spring Boot com Oracle**, chamando **procedures, functions e packages PL/SQL**.

A ideia: o cadastro de produtos e categorias usa JPA normal; a regra de negócio de pedidos (criar, adicionar item, baixar estoque, finalizar, cancelar) fica num **package PL/SQL** (`PKG_PEDIDOS`), e o Java chama esse package. É assim que muitos sistemas bancários e legados funcionam.

```
Controller ──► Service (@Transactional) ──► Repository ──► PKG_PEDIDOS (PL/SQL) ──► tabelas
                                                │
                                                └─► JPA (leitura e cadastros simples)
```

---

## O que mudou em relação à versão PostgreSQL

| Antes | Agora |
|---|---|
| Driver `postgresql` | Driver `ojdbc11` (Oracle) |
| Hibernate criava as tabelas (`ddl-auto=update`) | Tabelas criadas por scripts SQL (`ddl-auto=none`), como em empresa |
| `preco` em `Double` | `BigDecimal` / `NUMBER(10,2)` — dinheiro nunca em ponto flutuante |
| IDs manuais em Categoria e Pedido | Todos com `IDENTITY` |
| Classe `Principal` salvando dados fixos ao iniciar | API REST com controllers, services e DTOs |
| Sem relacionamentos | Produto → Categoria, Itens do pedido, log de auditoria |

---

## Passo a passo

### 1. Subir o Oracle no Docker

Na raiz do projeto:

```bash
docker compose up -d
docker logs -f oracle-pedidos
```

Espere aparecer **`DATABASE IS READY TO USE!`** (na primeira vez demora uns minutos por causa do download). `Ctrl+C` sai do log sem derrubar o banco.

O compose já cria o usuário da aplicação: **`pedidos` / `pedidos123`**.

### 2. Conectar no DBeaver

Nova conexão → **Oracle**:

| Campo | Valor |
|---|---|
| Host | `localhost` |
| Port | `1521` |
| Database | `FREEPDB1` |
| Tipo do Database | **Service Name** (não SID) |
| Username | `pedidos` |
| Password | `pedidos123` |

Se o DBeaver pedir para baixar o driver, aceite.

> Erro `ORA-12514`? Quase sempre é o campo marcado como SID em vez de Service Name.

### 3. Rodar os scripts, nesta ordem

Abra cada arquivo de `database/oracle/` no DBeaver (conectado como `pedidos`) e execute como **script** com **Alt+X**:

1. `01_tabelas.sql` — cria as tabelas, constraints e índices
2. `02_pkg_pedidos.sql` — cria o package. No final, o SELECT deve mostrar `PACKAGE` e `PACKAGE BODY` como **VALID**
3. `03_dados_exemplo.sql` — categorias e produtos
4. `04_testes.sql` — **não rode tudo de uma vez**: selecione um bloco por vez e execute com **Ctrl+Enter**. Ative o painel de saída (**Ctrl+Shift+O**) para ver o `DBMS_OUTPUT`

Se o package ficar INVALID: `SELECT * FROM user_errors WHERE name = 'PKG_PEDIDOS';` mostra a linha do erro.

Para recomeçar do zero, é só rodar o 01, 02 e 03 de novo.

### 4. Rodar a aplicação

```bash
cp .env.example .env
./mvnw spring-boot:run
```

> O teste `contextLoads` precisa do banco no ar. Para empacotar sem o banco: `./mvnw package -DskipTests`.

### 5. Testar a API

Use o arquivo `requests.http` (IntelliJ roda direto) ou copie para o Postman. Depois de cada chamada, olhe as tabelas no DBeaver — principalmente `PEDIDOS`, `ITENS_PEDIDO`, o `ESTOQUE` em `PRODUTOS` e a `LOG_PEDIDOS`.

| Endpoint | O que chama no banco |
|---|---|
| `POST /pedidos` | `PKG_PEDIDOS.CRIAR_PEDIDO` (parâmetro **OUT** devolve o id) |
| `POST /pedidos/{id}/itens` | `PKG_PEDIDOS.ADICIONAR_ITEM` |
| `POST /pedidos/{id}/finalizar` | `PKG_PEDIDOS.FINALIZAR_PEDIDO` via **`@Procedure`** |
| `POST /pedidos/{id}/cancelar` | `PKG_PEDIDOS.CANCELAR_PEDIDO` |
| `GET /pedidos/{id}` | JPA + **FUNCTION** `CALCULAR_TOTAL` |
| `GET /pedidos?cliente=luis` | procedure que devolve **SYS_REFCURSOR** |
| `GET/POST /produtos`, `/categorias` | JPA puro |

---

## Onde olhar no código

| Conceito | Arquivo |
|---|---|
| Package (spec + body), exceções, `FOR UPDATE`, transação autônoma, cursor | `database/oracle/02_pkg_pedidos.sql` |
| Chamar PL/SQL com `@Procedure` | `repository/PedidoRepository.java` |
| Chamar PL/SQL com `SimpleJdbcCall` (OUT, FUNCTION, REF CURSOR) | `repository/PedidoPlsqlRepository.java` |
| Transação controlada pelo Java | `service/PedidoService.java` |
| Erro `RAISE_APPLICATION_ERROR` virando status HTTP | `exception/GlobalExceptionHandler.java` |
| Entidades só de leitura (`@Immutable`) | `model/Pedido.java`, `model/ItemPedido.java` |

### Como um erro do PL/SQL chega na API

1. O package faz `RAISE_APPLICATION_ERROR(-20003, 'Estoque insuficiente...')`
2. O driver JDBC lança `SQLException` com `errorCode = 20003`
3. O Spring embrulha numa `DataAccessException` e o `@Transactional` faz **rollback**
4. O `GlobalExceptionHandler` acha a `SQLException`, lê o código e devolve **422** com a mensagem (20001/20002 viram **404**)

Códigos do projeto: `-20001` pedido não encontrado · `-20002` produto não encontrado · `-20003` estoque insuficiente · `-20004` pedido não está aberto · `-20005` dado inválido · `-20006` pedido sem itens.

---

## Exercícios para fazer sozinho

1. Crie no package uma procedure `remover_item(p_item_id)` que devolve o estoque e recalcula o total. Exponha em `DELETE /pedidos/{id}/itens/{itemId}`.
2. Crie uma function `estoque_disponivel(p_produto_id) RETURN NUMBER` e use num SELECT.
3. Troque o `IDENTITY` de `log_pedidos` por uma **SEQUENCE** (`CREATE SEQUENCE seq_log_pedidos`) e use `seq_log_pedidos.NEXTVAL` no insert.
4. Chame `finalizar_pedido` também via `SimpleJdbcCall` e compare com o `@Procedure`.
5. Abra duas abas no DBeaver com auto-commit desligado e tente adicionar item no mesmo pedido nas duas. Veja o `FOR UPDATE` segurando a segunda até a primeira dar COMMIT.

---

## Para a entrevista: o que este projeto te deixa falar

- **Procedure x Function:** function sempre retorna um valor e pode ser usada dentro de um SELECT; procedure executa uma ação e devolve dados por parâmetros OUT.
- **Package:** agrupa procedures e functions relacionadas. A *spec* é o contrato público (como uma interface Java); o *body* é a implementação e pode ter rotinas privadas.
- **Por que regra no banco?** Performance (processa perto dos dados, menos idas e vindas pela rede) e reaproveitamento entre sistemas. O contra: regra espalhada entre Java e banco, mais difícil de testar e versionar.
- **Tratamento de erro:** `EXCEPTION WHEN NO_DATA_FOUND`, `WHEN OTHERS`, `RAISE_APPLICATION_ERROR` com códigos de -20000 a -20999.
- **COMMIT dentro da procedure?** Neste projeto, não: quem controla a transação é o Java, para que várias chamadas sejam um tudo-ou-nada. A exceção é o log com `PRAGMA AUTONOMOUS_TRANSACTION`, que grava mesmo se a operação principal der rollback.
- **`FOR UPDATE`:** trava a linha para evitar que duas requisições alterem o mesmo pedido ou estoque ao mesmo tempo.
- **`SYS_REFCURSOR`:** jeito de uma procedure devolver uma lista de linhas.
- **Chamando do Java:** `@Procedure` (Spring Data JPA) para casos simples; `SimpleJdbcCall` quando há OUT, function ou cursor.
- **`%TYPE`:** a variável herda o tipo da coluna; se a coluna mudar, o código acompanha.
