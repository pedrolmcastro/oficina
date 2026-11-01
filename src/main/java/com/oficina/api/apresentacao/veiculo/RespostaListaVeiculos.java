package com.oficina.api.apresentacao.veiculo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.oficina.api.aplicacao.veiculo.PaginaVeiculos;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record RespostaListaVeiculos(
    List<RespostaVeiculo> veiculos,
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    @Schema(description = "ID do último veículo desta página para usar em after; null quando não há próxima página.",
        types = {
            "string",
            "null" }, format = "uuid")
    @Nullable
    UUID nextCursor
) {
    public RespostaListaVeiculos {
        veiculos = List.copyOf(veiculos);
    }

    public static RespostaListaVeiculos de(final PaginaVeiculos pagina) {
        return new RespostaListaVeiculos(
            pagina.veiculos().stream().map(RespostaVeiculo::de).toList(),
            pagina.nextCursor()
        );
    }
}
