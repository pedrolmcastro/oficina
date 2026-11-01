package com.oficina.api.infraestrutura.documentacao;

import com.oficina.api.ConfiguracaoIntegration;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(value = ConfiguracaoIntegration.class)
class SwaggerIT {
    @LocalServerPort
    private int porta;

    @ParameterizedTest
    @ValueSource(strings = { "/swagger-ui.html", "/swagger-ui/swagger-ui-bundle.js" })
    void deveDisponibilizarDocumentacao(final String caminho) {
        final var resposta = RestAssured.given().port(this.porta).get(caminho);
        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.asString()).isNotBlank();
    }

    @Test
    void deveDocumentarCrudDeVeiculos() {
        final var resposta = RestAssured.given().port(this.porta).get("/v3/api-docs");
        assertThat(resposta.statusCode()).isEqualTo(200);
        final var documento = JsonMapper.builder().build().readTree(resposta.asString());
        final var cadastro = documento.path("paths").path("/veiculos");
        final var porId = documento.path("paths").path("/veiculos/{id}");
        assertThat(cadastro.path("post").path("responses").has("201")).isTrue();
        assertThat(cadastro.path("post").path("responses").has("409")).isTrue();
        final var parametros = cadastro.path("get").path("parameters");
        assertThat(parametros)
            .extracting(parametro -> parametro.path("name").asString())
            .containsExactlyInAnyOrder("placa", "cliente", "after", "first");
        assertThat(parametros).allSatisfy(parametro -> {
            assertThat(parametro.path("required").asBoolean()).isFalse();
            if ("first".equals(parametro.path("name").asString())) {
                assertThat(parametro.path("schema").path("default").asInt()).isEqualTo(25);
                assertThat(parametro.path("schema").path("minimum").asInt()).isEqualTo(1);
                assertThat(parametro.path("schema").path("maximum").asInt()).isEqualTo(25);
            }
        });
        final var referencia = cadastro
            .path("get")
            .path("responses")
            .path("200")
            .path("content")
            .path("application/json")
            .path("schema")
            .path("$ref")
            .asString();
        assertThat(referencia).startsWith("#/components/schemas/");
        final var pagina = documento.at(referencia.substring(1));
        assertThat(pagina.path("properties").path("veiculos").path("type").asString()).isEqualTo("array");
        assertThat(pagina.path("properties").has("nextCursor")).isTrue();
        assertThat(porId.has("get")).isTrue();
        assertThat(porId.has("patch")).isTrue();
        assertThat(porId.has("delete")).isTrue();
        assertThat(porId.has("put")).isFalse();
        assertThat(porId.path("patch").path("requestBody").path("content").has("application/merge-patch+json"))
            .isTrue();
        assertThat(cadastro.path("post").path("security").get(0).has("bearerAuth")).isTrue();
    }

    @Test
    void deveGerarOpenApi() {
        final var resposta = RestAssured.given().port(this.porta).get("/v3/api-docs");
        assertThat(resposta.statusCode()).isEqualTo(200);
        final var documento = JsonMapper.builder().build().readTree(resposta.asString());
        assertThat(documento.path("openapi").asString()).startsWith("3.");
        assertThat(documento.path("info").isObject()).isTrue();
        assertThat(documento.path("paths").isObject()).isTrue();
    }
}
