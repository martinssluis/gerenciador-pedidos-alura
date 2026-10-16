package com.martinssluis.gerenciador_pedidos_alura.dto;

import java.time.LocalDateTime;

public record ErroResponse(int status, String codigoOracle, String mensagem, LocalDateTime momento) {
}
