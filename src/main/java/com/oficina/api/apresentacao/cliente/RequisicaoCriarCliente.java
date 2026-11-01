package com.oficina.api.apresentacao.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record RequisicaoCriarCliente(
    @Schema(example = "Maria Silva")
    @NotNull
    String nome,
    @Schema(example = "maria@example.com")
    @NotNull
    String email,
    @Schema(description = "CPF ou CNPJ, apenas dígitos", example = "52998224725")
    @NotNull
    String documento
) {}
