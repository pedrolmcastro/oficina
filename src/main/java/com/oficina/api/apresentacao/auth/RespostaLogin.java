package com.oficina.api.apresentacao.auth;

import io.swagger.v3.oas.annotations.media.Schema;

public record RespostaLogin(
    @Schema(description = "JWT usado no cabeçalho Authorization", requiredMode = Schema.RequiredMode.REQUIRED)
    String tokenAcesso,
    @Schema(description = "Tipo do token de acesso", requiredMode = Schema.RequiredMode.REQUIRED)
    TipoToken tipoToken,
    @Schema(
        description = "Tempo de validade do token em segundos",
        example = "1800",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    long expiraEm
) {}
