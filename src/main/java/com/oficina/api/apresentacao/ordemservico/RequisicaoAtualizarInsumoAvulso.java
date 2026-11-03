package com.oficina.api.apresentacao.ordemservico;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record RequisicaoAtualizarInsumoAvulso(
    @Schema(description = "Quantidade absoluta positiva de insumos", minimum = "1", example = "3")
    @NotNull
    Integer quantidade
) {}
