package com.oficina.api.apresentacao.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RequisicaoLogin(
    @Schema(example = "admin")
    @NotBlank
    String nomeUsuario,
    @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
    @NotBlank
    String senha
) {}
