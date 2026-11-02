package com.oficina.api.apresentacao.ordemservico;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RequisicaoCriarInsumoAvulso(
    @NotNull
    UUID insumoId,
    @NotNull
    Integer quantidade
) {}
