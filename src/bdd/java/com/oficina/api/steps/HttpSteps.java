package com.oficina.api.steps;

import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.assertj.core.api.Assertions.assertThat;

public class HttpSteps {
    @LocalServerPort
    private int porta;

    private @Nullable String token;
    private @Nullable Response resposta;

    public void autenticar(final String tokenAcesso) {
        this.token = tokenAcesso;
    }

    public RequestSpecification requisicao() {
        final var requisicao = RestAssured.given().port(this.porta).contentType("application/json");
        final var tokenAtual = this.token;
        return tokenAtual == null
            ? requisicao
            : requisicao.auth().oauth2(tokenAtual);
    }

    public void receber(final Response respostaRecebida) {
        this.resposta = respostaRecebida;
    }

    public Response resposta() {
        return Objects.requireNonNull(this.resposta);
    }

    @Then("a API deve retornar {int}")
    public void validarStatus(final int status) {
        assertThat(resposta().statusCode()).isEqualTo(status);
    }

    @Then("a resposta não deve conter conteúdo nem permitir cache")
    public void validarRespostaVaziaSemCache() {
        assertThat(resposta().body().asString()).isEmpty();
        assertThat(resposta().header("Cache-Control")).isEqualTo("no-store");
    }
}
