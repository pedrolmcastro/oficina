package com.oficina.api.apresentacao;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.oficina.api.aplicacao.Pagina;
import com.oficina.api.dominio.Entidade;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

class RespostaListaTest {
    @Test
    void deveConverterItensPreservandoCursor() {
        final Entidade entidade = new Entidade() {};
        final var pagina = new Pagina<>(List.of(entidade), entidade.id());
        final var resposta = RespostaLista.de(pagina, Entidade::id);
        assertThat(resposta.itens()).containsExactly(entidade.id());
        assertThat(resposta.nextCursor()).isEqualTo(entidade.id());
    }

    @Test
    void deveSerializarPaginaVaziaComCursorNulo() {
        final var json = JsonMapper
            .builder()
            .changeDefaultPropertyInclusion(inclusao -> inclusao.withValueInclusion(JsonInclude.Include.NON_NULL))
            .build();
        final var resposta = new RespostaLista<>(List.of(), null);
        assertThat(json.readTree(json.writeValueAsString(resposta)))
            .isEqualTo(json.readTree("{\"itens\":[],\"nextCursor\":null}"));
    }

    @Test
    void devePreservarItensMesmoSeOrigemForAlterada() {
        final var itens = new ArrayList<>(List.of("item"));
        final var resposta = new RespostaLista<>(itens, null);
        itens.clear();
        assertThat(resposta.itens()).containsExactly("item");
    }
}
