package com.oficina.api.apresentacao.ordemservico;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record RequisicaoCriarOrdemServico(
    @NotNull
    UUID veiculoId,
    @Schema(description = "IDs dos serviços cadastrados, sem duplicatas",
        example = "[\"019a0000-0000-7000-8000-000000000004\"]")
    @NotEmpty
    List<@NotNull UUID> servicoIds
) {
    public RequisicaoCriarOrdemServico {
        servicoIds = List.copyOf(servicoIds);
    }
}
