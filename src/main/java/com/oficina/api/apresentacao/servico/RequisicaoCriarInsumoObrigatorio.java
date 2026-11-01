package com.oficina.api.apresentacao.servico;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RequisicaoCriarInsumoObrigatorio(
    @NotNull
    UUID insumoId
) {}
