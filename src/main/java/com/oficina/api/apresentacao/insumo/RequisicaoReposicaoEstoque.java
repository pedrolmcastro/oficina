package com.oficina.api.apresentacao.insumo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record RequisicaoReposicaoEstoque(
    @Schema(description = "Quantidade positiva a adicionar ao estoque", minimum = "1", example = "10")
    @NotNull
    Integer quantidade
) {}
