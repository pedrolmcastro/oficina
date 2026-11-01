package com.oficina.api.apresentacao.servico;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.oficina.api.aplicacao.servico.PaginaServicos;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record RespostaListaServicos(
    List<RespostaServico> servicos,
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    @Schema(description = "ID do último serviço desta página para usar em after; null quando não há próxima página.",
        types = {
            "string",
            "null" }, format = "uuid")
    @Nullable
    UUID nextCursor
) {
    public RespostaListaServicos {
        servicos = List.copyOf(servicos);
    }

    public static RespostaListaServicos de(final PaginaServicos pagina) {
        return new RespostaListaServicos(
            pagina.servicos().stream().map(RespostaServico::de).toList(),
            pagina.nextCursor()
        );
    }
}
