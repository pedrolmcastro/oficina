package com.oficina.api.apresentacao.servico;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

@Schema(description = "Envie ao menos um campo não nulo. Campos omitidos ou nulos têm seu valor preservado.")
public record RequisicaoAtualizarServico(
    @Nullable
    String nome,
    @Nullable
    @Schema(description = "Valor não negativo, com no máximo duas casas decimais", minimum = "0", maximum = "10000.00",
        example = "19.90")
    BigDecimal precoVenda,
    @Nullable
    Boolean requerDiagnostico
) {
    @AssertTrue(message = "Informe ao menos um campo para atualizar")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isAlgumCampoDefinido() {
        return this.nome != null || this.precoVenda != null || this.requerDiagnostico != null;
    }
}
