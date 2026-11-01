package com.oficina.api.apresentacao.veiculo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RequisicaoCriarVeiculo(
    @Schema(description = "Placa antiga (ABC1234) ou Mercosul (ABC1D23), em maiúsculas e sem hífen",
        example = "ABC1D23")
    @NotNull
    String placa,
    @Schema(example = "Corolla")
    @NotNull
    String modelo,
    @Schema(example = "Toyota")
    @NotNull
    String marca,
    @Schema(description = "De 1886 até o ano atual mais um", example = "2024")
    @NotNull
    Integer ano,
    @NotNull
    UUID clienteId
) {}
