package com.oficina.api.apresentacao;

import com.oficina.api.ConfiguracaoIntegration;
import com.oficina.api.apresentacao.cliente.RespostaCliente;
import com.oficina.api.infraestrutura.seguranca.EmissorTokenJwt;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(value = ConfiguracaoIntegration.class)
class ClientesIT {
    @LocalServerPort
    private int porta;

    @Autowired
    private EmissorTokenJwt emissor;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limparClientes() {
        this.jdbc.update("DELETE FROM oficina.veiculos");
        this.jdbc.update("DELETE FROM oficina.clientes");
    }

    @Test
    void devePreservarCadastroAposAtualizacaoInvalida() {
        final var original = criarCliente();

        final var resposta = requisicao()
            .body(Map.of("nome", "Novo nome", "email", "novo@example.com", "documento", "11111111111"))
            .patch("/clientes/{id}", original.id());

        assertThat(resposta.statusCode()).isEqualTo(400);
        assertThat(requisicao().get("/clientes/{id}", original.id()).as(RespostaCliente.class)).isEqualTo(original);
    }

    @ParameterizedTest
    @ValueSource(strings = { "PATCH", "DELETE" })
    void deveRetornar404ParaIdExcluido(final String metodo) {
        final var cliente = criarCliente();
        assertThat(requisicao().delete("/clientes/{id}", cliente.id()).statusCode()).isEqualTo(204);

        final var resposta = requisicao()
            .body(Map.of("nome", "Novo nome"))
            .request(metodo, "/clientes/{id}", cliente.id());

        assertThat(resposta.statusCode()).isEqualTo(404);
    }

    @Test
    void deveRetornar404ParaDocumentoExcluido() {
        final var cliente = criarCliente();
        assertThat(requisicao().delete("/clientes/{id}", cliente.id()).statusCode()).isEqualTo(204);

        final var resposta = requisicao().queryParam("documento", cliente.documento()).get("/clientes");

        assertThat(resposta.statusCode()).isEqualTo(404);
    }

    @Test
    void deveRespeitarContratoHttpNoCicloDeVidaDoCliente() {
        final var cadastro = requisicao()
            .body(Map.of("nome", "Maria Silva", "email", "maria@example.com", "documento", "52998224725"))
            .post("/clientes");
        validarSemCache(cadastro, 201);
        final var cliente = cadastro.as(RespostaCliente.class);
        assertThat(cadastro.header("Location")).endsWith("/clientes/" + cliente.id());
        validarSemCache(requisicao().get("/clientes/{id}", cliente.id()), 200);
        validarSemCache(requisicao().queryParam("documento", cliente.documento()).get("/clientes"), 200);
        validarSemCache(requisicao().body(Map.of("nome", "Novo nome")).patch("/clientes/{id}", cliente.id()), 200);
        final var exclusao = requisicao().delete("/clientes/{id}", cliente.id());
        validarSemCache(exclusao, 204);
        assertThat(exclusao.body().asString()).isEmpty();
    }

    private static void validarSemCache(final Response resposta, final int status) {
        assertThat(resposta.statusCode()).isEqualTo(status);
        assertThat(resposta.header("Cache-Control")).isEqualTo("no-store");
    }

    private RespostaCliente criarCliente() {
        final var resposta = requisicao()
            .body(Map.of("nome", "Maria Silva", "email", "maria@example.com", "documento", "52998224725"))
            .post("/clientes");
        assertThat(resposta.statusCode()).isEqualTo(201);
        return resposta.as(RespostaCliente.class);
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
