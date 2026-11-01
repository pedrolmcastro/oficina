package com.oficina.api.apresentacao.auth;

import jakarta.validation.constraints.NotBlank;

public record RequisicaoLogin(@NotBlank String nomeUsuario, @NotBlank String senha) {}
