package com.oficina.api.apresentacao;

import com.oficina.api.ConfiguracaoIntegration;
import com.oficina.api.apresentacao.insumo.RespostaInsumo;
import com.oficina.api.apresentacao.insumo.RespostaListaInsumos;
import com.oficina.api.infraestrutura.seguranca.EmissorTokenJwt;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(value = ConfiguracaoIntegration.class)
class InsumosIT {
    @LocalServerPort
    private int porta;
    @Autowired
    private EmissorTokenJwt emissor;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void preparar() {
        this.jdbc.update("DELETE FROM oficina.servicos_insumos_obrigatorios");
        this.jdbc.update("DELETE FROM oficina.insumos");
    }

    @Test
    void deveRespeitarContratoHttpEPreservarHistorico() {
        final var cadastro = cadastrar("Óleo de motor");
        validar(cadastro, 201);
        final var original = cadastro.as(RespostaInsumo.class);
        assertThat(cadastro.header("Location")).isEqualTo("/insumos/" + original.id());
        validar(requisicao().get("/insumos/{id}", original.id()), 200);
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

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "ÓLEO" })
    void devePercorrerInsumosEmOrdemSemRepeticoes(final @Nullable String nome) {
        final var oleos = List
            .of(
                cadastrar("Filtro de óleo").as(RespostaInsumo.class),
                cadastrar("Óleo para motor").as(RespostaInsumo.class),
                cadastrar("Óleos sintéticos").as(RespostaInsumo.class)
            );
        final var todos = new ArrayList<>(oleos);
        todos.add(cadastrar("Pneu").as(RespostaInsumo.class));
        final var esperados = (nome == null
            ? todos
            : oleos).stream().sorted(Comparator.comparing(insumo -> insumo.id().toString())).toList();
        final Map<String, Object> parametros = new HashMap<>();
        parametros.put("first", 2);
        if (nome != null) {
            parametros.put("nome", nome);
        }
        final var primeira = listar(parametros);
        assertThat(primeira.insumos()).containsExactlyElementsOf(esperados.subList(0, 2));
        assertThat(primeira.nextCursor()).isEqualTo(esperados.get(1).id());
        parametros.put("after", primeira.nextCursor());
        final var ultima = listar(parametros);
        assertThat(ultima.insumos()).containsExactlyElementsOf(esperados.subList(2, esperados.size()));
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
            .isEqualTo(new RespostaInsumo(original.id(), original.nome(), new BigDecimal("0.00")));
        assertThat(requisicao().get("/insumos/{id}", original.id()).as(RespostaInsumo.class))
            .isEqualTo(resposta.as(RespostaInsumo.class));
    }

    private Response cadastrar(final String nome) {
        return requisicao().body(Map.of("nome", nome, "precoVenda", new BigDecimal("19.90"))).post("/insumos");
    }

    private RespostaListaInsumos listar(final Map<String, ?> parametros) {
        final var resposta = requisicao().queryParams(parametros).get("/insumos");
        validar(resposta, 200);
        return resposta.as(RespostaListaInsumos.class);
    }

    private static void validar(final Response resposta, final int status) {
        assertThat(resposta.statusCode()).as(resposta.body().asString()).isEqualTo(status);
        assertThat(resposta.header("Cache-Control")).isEqualTo("no-store");
    }

    private RequestSpecification requisicao() {
        return RestAssured
            .given()
            .port(this.porta)
            .contentType("application/json")
            .auth()
            .oauth2(this.emissor.emitir("admin", Set.of("ADMIN")).valor());
    }
}
