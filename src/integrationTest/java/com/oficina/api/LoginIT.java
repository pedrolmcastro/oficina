package com.oficina.api;

import io.restassured.RestAssured;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(value = ConfiguracaoIntegration.class)
class LoginIT {
    @LocalServerPort
    private int porta;

    @ParameterizedTest
    @ValueSource(strings = {
        "{ \"nomeUsuario\": \"\", \"senha\": \"admin\" }",
        "{ \"nomeUsuario\": \"admin\", \"senha\":\"   \" }",
        "{ \"senha\": \"admin\" }",
        "{ \"nomeUsuario\": \"admin\" }",
        "{}",
        "{" })
    void deveRejeitarPayloadInvalido(final String json) {
        final var resposta = RestAssured
            .given()
            .port(this.porta)
            .contentType("application/json")
            .body(json)
            .post("/login");

        assertThat(resposta.statusCode()).isEqualTo(400);
    }
}
