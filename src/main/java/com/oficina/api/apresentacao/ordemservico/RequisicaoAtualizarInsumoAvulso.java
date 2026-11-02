package com.oficina.api.apresentacao.ordemservico;

import jakarta.validation.constraints.NotNull;

public record RequisicaoAtualizarInsumoAvulso(
    @NotNull
    Integer quantidade
) {}
