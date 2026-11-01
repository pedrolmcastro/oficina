package com.oficina.api.apresentacao;

import com.oficina.api.ConfiguracaoIntegration;
import com.oficina.api.apresentacao.cliente.RespostaCliente;
import com.oficina.api.apresentacao.veiculo.RespostaVeiculo;
import com.oficina.api.infraestrutura.seguranca.EmissorTokenJwt;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
class VeiculosIT {
    @LocalServerPort
    private int porta;
    @Autowired
    private EmissorTokenJwt emissor;
    @Autowired
    private JdbcTemplate jdbc;
    private UUID clienteId;

    @BeforeEach
    void preparar() {
        this.jdbc.update("DELETE FROM oficina.veiculos");
        this.jdbc.update("DELETE FROM oficina.clientes");
        this.clienteId = criarCliente("52998224725");
    }

    @Test
    void deveRespeitarContratoHttpNoCicloDeVidaDoVeiculo() {
        final var cadastro = cadastrar("ABC1234");
        validarSemCache(cadastro, 201);
        final var veiculo = cadastro.as(RespostaVeiculo.class);
        assertThat(cadastro.header("Location")).endsWith("/veiculos/" + veiculo.id());
        validarSemCache(requisicao().get("/veiculos/{id}", veiculo.id()), 200);
        validarSemCache(requisicao().queryParam("placa", veiculo.placa()).get("/veiculos"), 200);
        validarSemCache(requisicao().body(Map.of("modelo", "Civic")).patch("/veiculos/{id}", veiculo.id()), 200);
        final var exclusao = requisicao().delete("/veiculos/{id}", veiculo.id());
        validarSemCache(exclusao, 204);
        assertThat(exclusao.body().asString()).isEmpty();
    }

    @Test
    void deveAtualizarSomenteClientePreservandoDemaisCampos() {
        final var original = cadastrar("ABC1234").as(RespostaVeiculo.class);
        final var novoCliente = criarCliente("11144477735");
        final var resposta = requisicao().body(Map.of("clienteId", novoCliente)).patch("/veiculos/{id}", original.id());
        validarSemCache(resposta, 200);
        final var esperado = new RespostaVeiculo(
            original.id(),
            original.placa(),
            original.modelo(),
            original.marca(),
            original.ano(),
            novoCliente
        );
        assertThat(resposta.as(RespostaVeiculo.class)).isEqualTo(esperado);
        assertThat(requisicao().get("/veiculos/{id}", original.id()).as(RespostaVeiculo.class)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @ValueSource(strings = { "PATCH", "DELETE" })
    void deveRetornar404ParaIdExcluido(final String metodo) {
        final var original = cadastrar("ABC1D23").as(RespostaVeiculo.class);
        assertThat(requisicao().delete("/veiculos/{id}", original.id()).statusCode()).isEqualTo(204);
        validarSemCache(
            requisicao().body(Map.of("modelo", "Civic")).request(metodo, "/veiculos/{id}", original.id()),
            404
        );
    }

    @Test
    void deveRetornar404ParaPlacaExcluida() {
        final var veiculo = cadastrar("ABC1234").as(RespostaVeiculo.class);
        assertThat(requisicao().delete("/veiculos/{id}", veiculo.id()).statusCode()).isEqualTo(204);

        final var resposta = requisicao().queryParam("placa", veiculo.placa()).get("/veiculos");

        assertThat(resposta.statusCode()).isEqualTo(404);
    }

    @Test
    void devePreservarCadastroAposAtualizacaoInvalida() {
        final var original = cadastrar("ABC1234").as(RespostaVeiculo.class);
        validarSemCache(
            requisicao().body(Map.of("modelo", "Civic", "placa", "invalida")).patch("/veiculos/{id}", original.id()),
            400
        );
        assertThat(requisicao().get("/veiculos/{id}", original.id()).as(RespostaVeiculo.class)).isEqualTo(original);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void deveRejeitarClienteInexistenteOuExcluido(final boolean excluido) {
        final var original = cadastrar("ABC1234").as(RespostaVeiculo.class);
        final UUID invalido;
        if (excluido) {
            invalido = criarCliente("11144477735");
            assertThat(requisicao().delete("/clientes/{id}", invalido).statusCode()).isEqualTo(204);
        } else {
            invalido = UUID.randomUUID();
        }
        validarSemCache(
            requisicao()
                .body(
                    Map.of("placa", "DEF1G23", "modelo", "Civic", "marca", "Honda", "ano", 2025, "clienteId", invalido)
                )
                .post("/veiculos"),
            404
        );
        validarSemCache(
            requisicao().body(Map.of("modelo", "Civic", "clienteId", invalido)).patch("/veiculos/{id}", original.id()),
            404
        );
        assertThat(requisicao().get("/veiculos/{id}", original.id()).as(RespostaVeiculo.class)).isEqualTo(original);
    }

    @ParameterizedTest
    @ValueSource(strings = { "POST", "GET", "PATCH", "DELETE" })
    void deveExigirAdministrador(final String metodo) {
        final var caminho = "POST".equals(metodo)
            ? "/veiculos"
            : "/veiculos/" + UUID.randomUUID();
        assertThat(RestAssured.given().port(this.porta).request(metodo, caminho).statusCode()).isEqualTo(401);
        assertThat(
            RestAssured
                .given()
                .port(this.porta)
                .auth()
                .oauth2(this.emissor.emitir("usuario", Set.of("USER")).valor())
                .request(metodo, caminho)
                .statusCode()
        ).isEqualTo(403);
    }

    private UUID criarCliente(final String documento) {
        final var resposta = requisicao()
            .body(Map.of("nome", "Maria", "email", "maria@example.com", "documento", documento))
            .post("/clientes");
        assertThat(resposta.statusCode()).isEqualTo(201);
        return resposta.as(RespostaCliente.class).id();
    }

    private Response cadastrar(final String placa) {
        return requisicao()
            .body(
                Map.of("placa", placa, "modelo", "Corolla", "marca", "Toyota", "ano", 2024, "clienteId", this.clienteId)
            )
            .post("/veiculos");
    }

    private static void validarSemCache(final Response resposta, final int status) {
        assertThat(resposta.statusCode()).isEqualTo(status);
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
