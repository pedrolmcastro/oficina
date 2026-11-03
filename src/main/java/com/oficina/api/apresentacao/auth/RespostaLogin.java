package com.oficina.api.apresentacao.auth;

import io.swagger.v3.oas.annotations.media.Schema;

public record RespostaLogin(
    @Schema(description = "Token de acesso JWT para uso no cabeçalho Authorization",
        requiredMode = Schema.RequiredMode.REQUIRED)
    String tokenAcesso,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    TipoToken tipoTokenAcesso,
    @Schema(description = "Validade do token de acesso em segundos", example = "1800",
        requiredMode = Schema.RequiredMode.REQUIRED)
    long validadeTokenAcesso
) {}
