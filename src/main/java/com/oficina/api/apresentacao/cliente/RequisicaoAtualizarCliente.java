package com.oficina.api.apresentacao.cliente;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import org.jspecify.annotations.Nullable;

@Schema(description = "Envie ao menos um campo não nulo. Campos omitidos ou nulos têm seu valor preservado.")
public record RequisicaoAtualizarCliente(
    @Schema(example = "Maria Silva")
    @Nullable
    String nome,
    @Schema(example = "maria@example.com")
    @Nullable
    String email,
    @Schema(description = "CPF ou CNPJ, apenas dígitos", example = "11144477735")
    @Nullable
    String documento
) {
    @AssertTrue(message = "Informe ao menos um campo para atualizar")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isAlgumCampoDefinido() {
        return this.nome != null || this.email != null || this.documento != null;
    }
}
