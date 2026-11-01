package com.oficina.api.apresentacao;

import com.oficina.api.ConfiguracaoIntegration;
import com.oficina.api.apresentacao.servico.RespostaListaServicos;
import com.oficina.api.apresentacao.servico.RespostaServico;
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
class ServicosIT {
    @LocalServerPort
    private int porta;
    @Autowired
    private EmissorTokenJwt emissor;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void preparar() {
        this.jdbc.update("DELETE FROM oficina.servicos_insumos_obrigatorios");
        this.jdbc.update("DELETE FROM oficina.servicos");
    }

    @Test
    void deveRespeitarContratoHttpEPreservarHistorico() {
        final var cadastro = cadastrar("Óleo de motor");
        validar(cadastro, 201);
        final var original = cadastro.as(RespostaServico.class);
        assertThat(cadastro.header("Location")).isEqualTo("/servicos/" + original.id());
        validar(requisicao().get("/servicos/{id}", original.id()), 200);
        validar(
            requisicao()
                .contentType("application/merge-patch+json")
                .body(Map.of("nome", original.nome()))
                .patch("/servicos/{id}", original.id()),
            200
        );
        validar(requisicao().delete("/servicos/{id}", original.id()), 204);
        for (final var metodo : List.of("PATCH", "DELETE")) {
            validar(requisicao().body(Map.of("nome", "Outro")).request(metodo, "/servicos/{id}", original.id()), 404);
        }
        assertThat(this.jdbc.queryForMap("SELECT nome, preco_venda FROM oficina.servicos WHERE id = ?", original.id()))
            .containsAllEntriesOf(Map.of("nome", original.nome(), "preco_venda", original.precoVenda()));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "ÓLEO" })
    void devePercorrerServicosEmOrdemSemRepeticoes(final @Nullable String nome) {
        final var oleos = List
            .of(
                cadastrar("Filtro de óleo").as(RespostaServico.class),
                cadastrar("Óleo para motor").as(RespostaServico.class),
                cadastrar("Óleos sintéticos").as(RespostaServico.class)
            );
        final var todos = new ArrayList<>(oleos);
        todos.add(cadastrar("Pneu").as(RespostaServico.class));
        final var esperados = (nome == null
            ? todos
            : oleos).stream().sorted(Comparator.comparing(servico -> servico.id().toString())).toList();
        final Map<String, Object> parametros = new HashMap<>();
        parametros.put("first", 2);
        if (nome != null) {
            parametros.put("nome", nome);
        }
        final var primeira = listar(parametros);
        assertThat(primeira.servicos()).containsExactlyElementsOf(esperados.subList(0, 2));
        assertThat(primeira.nextCursor()).isEqualTo(esperados.get(1).id());
        parametros.put("after", primeira.nextCursor());
        final var ultima = listar(parametros);
        assertThat(ultima.servicos()).containsExactlyElementsOf(esperados.subList(2, esperados.size()));
        assertThat(ultima.nextCursor()).isNull();
    }

    @Test
    void devePreservarNomeNuloEAceitarPrecoZero() {
        final var original = cadastrar("Óleo").as(RespostaServico.class);
        final var resposta = requisicao()
            .body("{\"nome\":null,\"precoVenda\":0}")
            .patch("/servicos/{id}", original.id());
        validar(resposta, 200);
        assertThat(resposta.as(RespostaServico.class))
            .isEqualTo(
                new RespostaServico(
                    original.id(),
                    original.nome(),
                    new BigDecimal("0.00"),
                    original.requerDiagnostico()
                )
            );
        assertThat(requisicao().get("/servicos/{id}", original.id()).as(RespostaServico.class))
            .isEqualTo(resposta.as(RespostaServico.class));
    }

    private Response cadastrar(final String nome) {
        return requisicao()
            .body(Map.of("nome", nome, "precoVenda", new BigDecimal("19.90"), "requerDiagnostico", true))
            .post("/servicos");
    }

    private RespostaListaServicos listar(final Map<String, ?> parametros) {
        final var resposta = requisicao().queryParams(parametros).get("/servicos");
        validar(resposta, 200);
        return resposta.as(RespostaListaServicos.class);
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
