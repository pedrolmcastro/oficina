package com.oficina.api.apresentacao.ordemservico;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record RequisicaoCriarOrdemServico(
    @NotNull
    UUID clienteId,
    @NotNull
    UUID veiculoId,
    @NotEmpty
    List<@NotNull UUID> servicoIds
) {
    public RequisicaoCriarOrdemServico {
        servicoIds = List.copyOf(servicoIds);
    }
}
