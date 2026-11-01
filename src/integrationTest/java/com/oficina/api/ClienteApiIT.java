package com.oficina.api;

import com.oficina.api.apresentacao.cliente.RespostaCliente;
import com.oficina.api.infraestrutura.seguranca.EmissorTokenJwt;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(value = ConfiguracaoIntegration.class)
class ClienteApiIT {
    @LocalServerPort
    private int porta;

    @Autowired
    private EmissorTokenJwt emissor;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limparClientes() {
        this.jdbc.update("DELETE FROM oficina.clientes");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "'', maria@example.com, 52998224725",
        "Maria, '', 52998224725",
        "Maria, invalido, 52998224725",
        "Maria, maria@example.com, ''",
        "Maria, maria@example.com, 123",
        "Maria, maria@example.com, 529.982.247-25",
        "Maria, maria@example.com, 11111111111",
        "Maria, maria@example.com, 52998224726",
        "Maria, maria@example.com, 00000000000000",
        "Maria, maria@example.com, 11222333000182",
        "Maria, maria@example.com, 11.222.333/0001-81" })
    void deveRetornar400ParaCadastroInvalido(final String nome, final String email, final String documento) {
        final var resposta = requisicao()
            .body(Map.of("nome", nome, "email", email, "documento", documento))
            .post("/clientes");

        assertThat(resposta.statusCode()).isEqualTo(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{",
        "{}",
        "{\"nome\":\"Maria\"}",
        "{\"nome\":null,\"email\":\"maria@example.com\",\"documento\":\"52998224725\"}",
        "{\"nome\":\"Maria\",\"email\":null,\"documento\":\"52998224725\"}",
        "{\"nome\":\"Maria\",\"email\":\"maria@example.com\",\"documento\":null}",
        "{\"nome\":\"Maria\",\"email\":\"maria@example.com\",\"documento\":\"52998224725\",\"extra\":1}" })
    void deveRetornar400ParaPayloadDeCadastroInvalido(final String json) {
        final var resposta = requisicao().body(json).post("/clientes");

        assertThat(resposta.statusCode()).isEqualTo(400);
        assertThat(resposta.contentType()).startsWith("application/problem+json");
        assertThat(resposta.jsonPath().getInt("status")).isEqualTo(400);
        assertThat(resposta.jsonPath().getString("detail")).isEqualTo("Requisição inválida");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "123",
        "529.982.247-25",
        "11111111111",
        "52998224726",
        "00000000000000",
        "11222333000182" })
    void deveRetornar400ParaConsultaComDocumentoInvalido(final String documento) {
        final var resposta = requisicao().queryParam("documento", documento).get("/clientes");

        assertThat(resposta.statusCode()).isEqualTo(400);
    }

    @Test
    void deveExigirParametroDocumento() {
        assertThat(requisicao().get("/clientes").statusCode()).isEqualTo(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{",
        "{}",
        "{\"nome\":null,\"email\":null,\"documento\":null}",
        "{\"nome\":\" \"}",
        "{\"email\":\"\"}",
        "{\"email\":\"invalido\"}",
        "{\"documento\":\"123\"}",
        "{\"documento\":\"52998224726\"}",
        "{\"campoDesconhecido\":\"valor\"}",
        "{\"nome\":\"Novo nome\",\"email\":\"novo@example.com\",\"documento\":\"11111111111\"}" })
    void deveRetornar400SemSalvarAtualizacaoInvalida(final String json) {
        final var original = criarCliente();

        final var resposta = requisicao().body(json).patch("/clientes/{id}", original.id());

        assertThat(resposta.statusCode()).isEqualTo(400);
        final var consulta = requisicao().get("/clientes/{id}", original.id());
        assertThat(consulta.statusCode()).isEqualTo(200);
        assertThat(consulta.as(RespostaCliente.class)).isEqualTo(original);
    }

    @ParameterizedTest
    @MethodSource(value = "atualizacoesParciais")
    void devePreservarCamposOmitidosENulos(final String formato, final String json) {
        final var original = criarCliente();

        final var resposta = requisicao().contentType(formato).body(json).patch("/clientes/{id}", original.id());

        assertThat(resposta.statusCode()).isEqualTo(200);
        final var esperado = new RespostaCliente(
            original.id(),
            original.nome(),
            "novo@example.com",
            original.tipoDocumento(),
            original.documento()
        );
        assertThat(resposta.as(RespostaCliente.class)).isEqualTo(esperado);
        final var consulta = requisicao().get("/clientes/{id}", original.id());
        assertThat(consulta.statusCode()).isEqualTo(200);
        assertThat(consulta.as(RespostaCliente.class)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @ValueSource(strings = { "GET", "PATCH", "DELETE" })
    void deveRetornar404ParaIdInexistente(final String metodo) {
        final var resposta = requisicao()
            .body(Map.of("nome", "Novo nome"))
            .request(metodo, "/clientes/{id}", UUID.randomUUID());

        assertThat(resposta.statusCode()).isEqualTo(404);
    }

    @ParameterizedTest
    @ValueSource(strings = { "GET", "PATCH", "DELETE" })
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
    void deveInformarLocalizacaoDoCadastro() {
        final var resposta = requisicao()
            .body(Map.of("nome", "Maria Silva", "email", "maria@example.com", "documento", "52998224725"))
            .post("/clientes");

        assertThat(resposta.statusCode()).isEqualTo(201);
        assertThat(resposta.header("Location")).endsWith("/clientes/" + resposta.as(RespostaCliente.class).id());
    }

    @Test
    void deveImpedirCacheDasRespostasDeClientes() {
        final var cadastro = requisicao()
            .body(Map.of("nome", "Maria Silva", "email", "maria@example.com", "documento", "52998224725"))
            .post("/clientes");
        validarSemCache(cadastro, 201);
        final var cliente = cadastro.as(RespostaCliente.class);
        validarSemCache(requisicao().get("/clientes/{id}", cliente.id()), 200);
        validarSemCache(requisicao().queryParam("documento", cliente.documento()).get("/clientes"), 200);
        validarSemCache(requisicao().body(Map.of("nome", "Novo nome")).patch("/clientes/{id}", cliente.id()), 200);
        final var exclusao = requisicao().delete("/clientes/{id}", cliente.id());
        validarSemCache(exclusao, 204);
        assertThat(exclusao.body().asString()).isEmpty();
    }

    private static Stream<Arguments> atualizacoesParciais() {
        return Stream
            .of("application/json", "application/merge-patch+json")
            .flatMap(
                formato -> Stream
                    .of(
                        "{\"email\":\"novo@example.com\"}",
                        "{\"email\":\"novo@example.com\",\"nome\":null,\"documento\":null}"
                    )
                    .map(json -> Arguments.of(formato, json))
            );
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
