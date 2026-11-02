package com.oficina.api.apresentacao.insumo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record RequisicaoEntradaEstoque(
    @Schema(description = "Quantidade inteira positiva a adicionar ao estoque", minimum = "1", example = "10")
    @NotNull
    Integer quantidade
) {}
