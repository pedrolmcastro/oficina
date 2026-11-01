package com.oficina.api.apresentacao.veiculo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Schema(description = "Envie ao menos um campo não nulo. Campos omitidos ou nulos têm seu valor preservado.")
public record RequisicaoAtualizarVeiculo(
    @Schema(description = "Placa antiga ou Mercosul, em maiúsculas e sem hífen", example = "ABC1D23")
    @Nullable
    String placa,
    @Schema(example = "Corolla")
    @Nullable
    String modelo,
    @Schema(example = "Toyota")
    @Nullable
    String marca,
    @Schema(description = "De 1886 até o ano atual mais um", example = "2024")
    @Nullable
    Integer ano,
    @Nullable
    UUID clienteId
) {
    @AssertTrue(message = "Informe ao menos um campo para atualizar")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isAlgumCampoDefinido() {
        return this.placa != null
            || this.modelo != null
            || this.marca != null
            || this.ano != null
            || this.clienteId != null;
    }
}
