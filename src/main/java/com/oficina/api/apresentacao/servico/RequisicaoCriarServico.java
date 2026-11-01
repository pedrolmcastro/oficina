package com.oficina.api.apresentacao.servico;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RequisicaoCriarServico(
    @Schema(example = "Troca de óleo")
    @NotNull
    String nome,
    @Schema(description = "Valor não negativo, até 10000.00, com no máximo duas casas decimais", minimum = "0",
        maximum = "10000.00", example = "19.90")
    @NotNull
    BigDecimal precoVenda,
    @Schema(description = "Indica se o serviço exige diagnóstico prévio", example = "false")
    @NotNull
    Boolean requerDiagnostico
) {}
