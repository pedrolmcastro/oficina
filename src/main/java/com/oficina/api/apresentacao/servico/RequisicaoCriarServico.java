package com.oficina.api.apresentacao.servico;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RequisicaoCriarServico(
    @Schema(example = "Troca de óleo")
    @NotNull
    String nome,
    @Schema(description = "Preço de venda do serviço com no máximo 2 casas decimais", minimum = "0",
        maximum = "10000.00", example = "19.90")
    @NotNull
    BigDecimal precoVenda,
    @NotNull
    Boolean requerDiagnostico
) {}
