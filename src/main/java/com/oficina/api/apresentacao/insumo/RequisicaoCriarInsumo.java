package com.oficina.api.apresentacao.insumo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RequisicaoCriarInsumo(
    @Schema(example = "Óleo de motor")
    @NotNull
    String nome,
    @Schema(description = "Valor não negativo, até 10000.00, com no máximo duas casas decimais", minimum = "0",
        maximum = "10000.00", example = "19.90")
    @NotNull
    BigDecimal precoVenda
) {}
