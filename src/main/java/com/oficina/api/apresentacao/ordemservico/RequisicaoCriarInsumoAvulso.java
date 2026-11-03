package com.oficina.api.apresentacao.ordemservico;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RequisicaoCriarInsumoAvulso(
    @NotNull
    UUID insumoId,
    @Schema(description = "Quantidade positiva de insumos", minimum = "1", example = "2")
    @NotNull
    Integer quantidade
) {}
