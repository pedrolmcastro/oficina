package com.oficina.api.apresentacao;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.oficina.api.aplicacao.Pagina;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

public record RespostaLista<T>(
    List<T> itens,
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    @Schema(description = "ID do último item desta página para usar em after; null quando não há próxima página.",
        types = {
            "string",
            "null" }, format = "uuid")
    @Nullable
    UUID nextCursor
) {
    public RespostaLista {
        itens = List.copyOf(itens);
    }

    public static <E, T> RespostaLista<T> de(final Pagina<E> pagina, final Function<? super E, T> conversor) {
        return new RespostaLista<>(pagina.itens().stream().map(conversor).toList(), pagina.nextCursor());
    }
}
