package com.oficina.api.apresentacao.servico;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record RequisicaoAtualizarInsumoObrigatorio(
    @Schema(description = "Quantidade absoluta positiva de insumos", example = "4")
    @NotNull
    Integer quantidade
) {}
