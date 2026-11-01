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
    void deveGerarOpenApi() {
        final var resposta = RestAssured.given().port(this.porta).get("/v3/api-docs");
        assertThat(resposta.statusCode()).isEqualTo(200);
        final var documento = JsonMapper.builder().build().readTree(resposta.asString());
        assertThat(documento.path("openapi").asString()).startsWith("3.");
        assertThat(documento.path("info").isObject()).isTrue();
        assertThat(documento.path("paths").isObject()).isTrue();
    }
}
