package com.oficina.api.apresentacao;

import com.oficina.api.apresentacao.insumo.RespostaInsumo;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

class InsumosIT extends ApiIntegrationTest {
    @BeforeEach
    void preparar() {
        this.jdbc.update("DELETE FROM oficina.servicos_insumos_obrigatorios");
        this.jdbc.update("DELETE FROM oficina.insumos");
    }

    @Test
    void deveRetornarLocalizacaoAceitarMergePatchEPreservarExclusaoLogica() {
        final var cadastro = cadastrar("Óleo de motor");
        validar(cadastro, 201);
        final var original = cadastro.as(RespostaInsumo.class);
        assertThat(cadastro.header("Location")).isEqualTo("/insumos/" + original.id());
        validar(
            requisicao()
                .contentType("application/merge-patch+json")
                .body(Map.of("nome", original.nome()))
                .patch("/insumos/{id}", original.id()),
            200
        );
        validar(requisicao().delete("/insumos/{id}", original.id()), 204);
        for (final var metodo : List.of("PATCH", "DELETE")) {
            validar(requisicao().body(Map.of("nome", "Outro")).request(metodo, "/insumos/{id}", original.id()), 404);
        }
        assertThat(this.jdbc.queryForMap("SELECT nome, preco_venda FROM oficina.insumos WHERE id = ?", original.id()))
            .containsAllEntriesOf(Map.of("nome", original.nome(), "preco_venda", original.precoVenda()));
    }

    @Test
    void devePaginarInsumosMantendoFiltroPorNome() {
        final var oleos = List
            .of(
                cadastrar("Filtro de óleo").as(RespostaInsumo.class),
                cadastrar("Óleo para motor").as(RespostaInsumo.class),
                cadastrar("Óleos sintéticos").as(RespostaInsumo.class)
            );
        cadastrar("Pneu");
        final var esperados = oleos.stream().sorted(Comparator.comparing(insumo -> insumo.id().toString())).toList();
        final Map<String, Object> parametros = new HashMap<>();
        parametros.put("first", 2);
        parametros.put("nome", "ÓLEO");
        final var primeira = listar(parametros);
        assertThat(primeira.itens()).containsExactlyElementsOf(esperados.subList(0, 2));
        assertThat(primeira.nextCursor()).isEqualTo(esperados.get(1).id());
        parametros.put("after", primeira.nextCursor());
        final var ultima = listar(parametros);
        assertThat(ultima.itens()).containsExactlyElementsOf(esperados.subList(2, esperados.size()));
        assertThat(ultima.nextCursor()).isNull();
    }

    @Test
    void devePreservarNomeNuloEAceitarPrecoZero() {
        final var original = cadastrar("Óleo").as(RespostaInsumo.class);
        final var resposta = requisicao()
            .body("{\"nome\":null,\"precoVenda\":0}")
            .patch("/insumos/{id}", original.id());
        validar(resposta, 200);
        assertThat(resposta.as(RespostaInsumo.class))
            .isEqualTo(new RespostaInsumo(original.id(), original.nome(), new BigDecimal("0.00"), 0, 0, 0));
        assertThat(requisicao().get("/insumos/{id}", original.id()).as(RespostaInsumo.class))
            .isEqualTo(resposta.as(RespostaInsumo.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"quantidade\":null}",
        "{\"quantidade\":1.5}",
        "{\"quantidade\":2147483648}",
        "{\"quantidade\":1,\"extra\":true}" })
    void deveValidarEntradaDeEstoque(final String corpo) {
        final var insumo = cadastrar("Óleo").as(RespostaInsumo.class);
        validar(requisicao().body(corpo).post("/insumos/{id}/reposicoes-estoque", insumo.id()), 400);
        assertThat(requisicao().get("/insumos/{id}", insumo.id()).as(RespostaInsumo.class)).isEqualTo(insumo);
    }

    @Test
    void deveRecusarEntradaEmInsumoExcluido() {
        final var insumo = cadastrar("Óleo").as(RespostaInsumo.class);
        validar(requisicao().delete("/insumos/{id}", insumo.id()), 204);
        validar(requisicao().body(Map.of("quantidade", 1)).post("/insumos/{id}/reposicoes-estoque", insumo.id()), 404);
        assertThat(
            this.jdbc
                .queryForMap(
                    "SELECT deletado, quantidade_estoque_fisico FROM oficina.insumos WHERE id = ?",
                    insumo.id()
                )
        ).containsAllEntriesOf(Map.of("deletado", true, "quantidade_estoque_fisico", 0));
    }

    @Test
    void deveRecusarOverflowDeEstoqueSemAlterarSaldo() {
        final var insumo = cadastrar("Óleo").as(RespostaInsumo.class);
        validar(
            requisicao()
                .body(Map.of("quantidade", Integer.MAX_VALUE))
                .post("/insumos/{id}/reposicoes-estoque", insumo.id()),
            200
        );
        validar(requisicao().body(Map.of("quantidade", 1)).post("/insumos/{id}/reposicoes-estoque", insumo.id()), 400);
        assertThat(requisicao().get("/insumos/{id}", insumo.id()).as(RespostaInsumo.class).quantidadeEstoqueFisico())
            .isEqualTo(Integer.MAX_VALUE);
    }

    private Response cadastrar(final String nome) {
        return requisicao().body(Map.of("nome", nome, "precoVenda", new BigDecimal("19.90"))).post("/insumos");
    }

    private RespostaLista<RespostaInsumo> listar(final Map<String, ?> parametros) {
        final var resposta = requisicao().queryParams(parametros).get("/insumos");
        validar(resposta, 200);
        return resposta.as(new TypeRef<RespostaLista<RespostaInsumo>>() {});
    }
}
