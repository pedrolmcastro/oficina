package com.oficina.api.apresentacao.insumo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.oficina.api.aplicacao.insumo.PaginaInsumos;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record RespostaListaInsumos(
    List<RespostaInsumo> insumos,
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    @Schema(description = "ID do último insumo desta página para usar em after; null quando não há próxima página.",
        types = {
            "string",
            "null" }, format = "uuid")
    @Nullable
    UUID nextCursor
) {
    public RespostaListaInsumos {
        insumos = List.copyOf(insumos);
    }

    public static RespostaListaInsumos de(final PaginaInsumos pagina) {
        return new RespostaListaInsumos(
            pagina.insumos().stream().map(RespostaInsumo::de).toList(),
            pagina.nextCursor()
        );
    }
}
