package com.oficina.api.apresentacao.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RequisicaoLogin(
    @Schema(description = "Nome de usuário cadastrado", example = "admin")
    @NotBlank
    String nomeUsuario,
    @Schema(description = "Senha do usuário", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
    @NotBlank
    String senha
) {}
