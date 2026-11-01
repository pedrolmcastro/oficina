package com.oficina.api.infraestrutura.persistencia;

import com.oficina.ConfiguracaoJpaIntegration;
import com.oficina.ConfiguracaoJpaIntegration.EntidadeTeste;
import com.oficina.api.ConfiguracaoIntegration;
import com.oficina.api.aplicacao.Pagina;
import jakarta.persistence.EntityManager;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.default_schema=public" })
@ContextConfiguration(classes = ConfiguracaoJpaIntegration.class)
@Import(value = ConfiguracaoIntegration.class)
class BuscaTextualIT {
    @Autowired
    private EntityManager entityManager;

    private final EntidadeTeste oleo = new EntidadeTeste("Óleos sintéticos", null);
    private final EntidadeTeste filtro = new EntidadeTeste("Filtro", "Óleo de motor");
    private final EntidadeTeste pneu = new EntidadeTeste("Pneu", "Borracha");

    @BeforeEach
    void cadastrar() {
        this.entityManager.persist(this.oleo);
        this.entityManager.persist(this.filtro);
        this.entityManager.persist(this.pneu);
        this.entityManager.flush();
    }

    @Test
    void devePesquisarEmUmaOuVariasColunasComValoresNulos() {
        final var limite = new Pagina.Limite(25);
        assertThat(busca(List.of("e.titulo")).fulltextSearch("ÓLEO", null, limite)).containsExactly(this.oleo);
        assertThat(busca(List.of("e.descricao")).fulltextSearch("ÓLEO", null, limite)).containsExactly(this.filtro);
        final var busca = busca(List.of("e.titulo", "e.descricao"));
        assertThat(busca.fulltextSearch("ÓLEO", null, limite))
            .containsExactlyElementsOf(ordenar(this.oleo, this.filtro));
        assertThat(busca.fulltextSearch("filtro óleo", null, limite)).containsExactly(this.filtro);
        assertThat(busca.fulltextSearch("inexistente", null, limite)).isEmpty();
        assertThat(busca.fulltextSearch("sint", null, limite)).isEmpty();
    }

    @Test
    void devePaginarSemFiltroComColunaDeCursorQualificada() {
        final var busca = busca(List.of("e.titulo"));
        final var limite = new Pagina.Limite(2);
        final var esperados = ordenar(this.oleo, this.filtro, this.pneu);
        final var encontrados = busca.fulltextSearch(null, null, limite);
        assertThat(encontrados).containsExactlyElementsOf(esperados);
        final var primeira = Pagina.de(encontrados, limite);
        assertThat(primeira.itens()).containsExactlyElementsOf(esperados.subList(0, 2));
        assertThat(busca.fulltextSearch(null, primeira.nextCursor(), limite)).containsExactly(esperados.getLast());
    }

    @Test
    void deveRespeitarLimiteDaConsulta() {
        assertThat(busca(List.of("e.titulo")).fulltextSearch(null, null, new Pagina.Limite(1))).hasSize(2);
    }

    @Test
    void deveExigirColunasPesquisaveis() {
        assertThatThrownBy(() -> busca(List.of())).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    private BuscaTextual<EntidadeTeste> busca(final List<String> colunas) {
        return new BuscaTextual<>(
            this.entityManager,
            EntidadeTeste.class,
            "SELECT e.* FROM public.entidades_teste e WHERE 1 = 1",
            "e.id",
            colunas
        );
    }

    private static List<EntidadeTeste> ordenar(final EntidadeTeste... entidades) {
        return List.of(entidades).stream().sorted(Comparator.comparing(entidade -> entidade.id().toString())).toList();
    }
}
