package com.oficina.api.apresentacao.servico;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RequisicaoCriarInsumoObrigatorio(
    @NotNull
    UUID insumoId,
    @Schema(description = "Quantidade positiva de insumos", minimum = "1", example = "4")
    @NotNull
    Integer quantidade
) {}
