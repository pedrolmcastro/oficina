package com.oficina.api.apresentacao.servico;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.oficina.api.aplicacao.servico.PaginaInsumosObrigatorios;
import com.oficina.api.apresentacao.insumo.RespostaInsumo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record RespostaListaInsumosObrigatorios(
    List<RespostaInsumo> insumosObrigatorios,
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    @Schema(description = "ID do último insumo obrigatório para usar em after; null quando não há próxima página.",
        types = {
            "string",
            "null" }, format = "uuid")
    @Nullable
    UUID nextCursor
) {
    public RespostaListaInsumosObrigatorios {
        insumosObrigatorios = List.copyOf(insumosObrigatorios);
    }

    public static RespostaListaInsumosObrigatorios de(final PaginaInsumosObrigatorios pagina) {
        return new RespostaListaInsumosObrigatorios(
            pagina.insumosObrigatorios().stream().map(RespostaInsumo::de).toList(),
            pagina.nextCursor()
        );
    }
}
