package com.martinssluis.gerenciador_pedidos_alura.exception;

import com.martinssluis.gerenciador_pedidos_alura.dto.ErroResponse;
import jakarta.persistence.PersistenceException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Traduz erros em respostas HTTP.
 *
 * O ponto principal: quando o PL/SQL faz RAISE_APPLICATION_ERROR(-20003, '...'),
 * o Java recebe uma SQLException com errorCode = 20003, embrulhada em
 * excecoes do Spring/JPA. Aqui a gente procura essa SQLException na "cadeia de causas"
 * e escolhe o status HTTP de acordo com o codigo do erro de negocio.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final int ERRO_PEDIDO_NAO_ENCONTRADO = 20001;
    private static final int ERRO_PRODUTO_NAO_ENCONTRADO = 20002;

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex) {
        return resposta(HttpStatus.NOT_FOUND, null, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> violacaoDeRegraDoBanco(DataIntegrityViolationException ex) {
        // Ex.: UNIQUE (nome do produto repetido), CHECK (preco <= 0), FK invalida.
        String mensagem = buscarErroOracle(ex)
                .map(GlobalExceptionHandler::primeiraLinha)
                .orElse("Violacao de regra do banco de dados.");
        return resposta(HttpStatus.CONFLICT, null, mensagem);
    }

    // DataAccessException: erros vindos do JdbcTemplate/SimpleJdbcCall e dos repositories JPA.
    @ExceptionHandler({DataAccessException.class, PersistenceException.class})
    public ResponseEntity<ErroResponse> erroDoBanco(RuntimeException ex) {
        Optional<SQLException> erroOracle = buscarErroOracle(ex)
                .filter(sql -> sql.getErrorCode() >= 20000 && sql.getErrorCode() <= 20999);

        if (erroOracle.isEmpty()) {
            return resposta(HttpStatus.INTERNAL_SERVER_ERROR, null, "Erro inesperado no banco de dados.");
        }

        SQLException sql = erroOracle.get();
        HttpStatus status = switch (sql.getErrorCode()) {
            case ERRO_PEDIDO_NAO_ENCONTRADO, ERRO_PRODUTO_NAO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.UNPROCESSABLE_CONTENT; // 422: regra de negocio violada
        };
        String mensagemSemPrefixo = primeiraLinha(sql).replaceFirst("^ORA-\\d{5}:\\s*", "");
        return resposta(status, "ORA-" + sql.getErrorCode(), mensagemSemPrefixo);
    }

    private static Optional<SQLException> buscarErroOracle(Throwable ex) {
        Throwable atual = ex;
        while (atual != null) {
            if (atual instanceof SQLException sql) {
                return Optional.of(sql);
            }
            atual = atual.getCause();
        }
        return Optional.empty();
    }

    // A mensagem do Oracle vem com varias linhas (ORA-20003 ... ORA-06512 at ...).
    // Para o cliente da API, so a primeira interessa.
    private static String primeiraLinha(SQLException sql) {
        String mensagem = sql.getMessage() == null ? "" : sql.getMessage();
        return mensagem.lines().findFirst().orElse(mensagem).trim();
    }

    private static ResponseEntity<ErroResponse> resposta(HttpStatus status, String codigoOracle, String mensagem) {
        return ResponseEntity.status(status)
                .body(new ErroResponse(status.value(), codigoOracle, mensagem, LocalDateTime.now()));
    }
}
