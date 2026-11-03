package com.oficina.api.apresentacao;

import com.oficina.api.apresentacao.cliente.RespostaCliente;
import com.oficina.api.apresentacao.veiculo.RespostaVeiculo;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

class VeiculosIT extends ApiIntegrationTest {
    private UUID clienteId;

    @BeforeEach
    void preparar() {
        this.jdbc.update("DELETE FROM oficina.veiculos");
        this.jdbc.update("DELETE FROM oficina.clientes");
        this.clienteId = criarCliente("52998224725");
    }

    @Test
    void deveRetornarLocalizacaoNoCadastroECorpoVazioNaExclusao() {
        final var cadastro = cadastrar("ABC1234");
        validar(cadastro, 201);
        final var veiculo = cadastro.as(RespostaVeiculo.class);
        assertThat(cadastro.header("Location")).endsWith("/veiculos/" + veiculo.id());
        final var exclusao = requisicao().delete("/veiculos/{id}", veiculo.id());
        validar(exclusao, 204);
        assertThat(exclusao.body().asString()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "PATCH", "DELETE" })
    void deveRetornar404ParaIdExcluido(final String metodo) {
        final var original = cadastrar("ABC1D23").as(RespostaVeiculo.class);
        assertThat(requisicao().delete("/veiculos/{id}", original.id()).statusCode()).isEqualTo(204);
        validar(requisicao().body(Map.of("modelo", "Civic")).request(metodo, "/veiculos/{id}", original.id()), 404);
    }

    @Test
    void deveCombinarFiltrosDePlacaECliente() {
        final var primeiro = cadastrar("ABC1234").as(RespostaVeiculo.class);
        final var outroCliente = criarCliente("11144477735");
        assertThat(listar(Map.of("placa", primeiro.placa(), "cliente", this.clienteId)).itens())
            .containsExactly(primeiro);
        assertThat(listar(Map.of("placa", primeiro.placa(), "cliente", outroCliente)).itens()).isEmpty();
    }

    @Test
    void deveRejeitarClienteExcluido() {
        final var original = cadastrar("ABC1234").as(RespostaVeiculo.class);
        final var invalido = criarCliente("11144477735");
        validar(requisicao().delete("/clientes/{id}", invalido), 204);
        validar(
            requisicao()
                .body(
                    Map.of("placa", "DEF1G23", "modelo", "Civic", "marca", "Honda", "ano", 2025, "clienteId", invalido)
                )
                .post("/veiculos"),
            404
        );
        validar(
            requisicao().body(Map.of("modelo", "Civic", "clienteId", invalido)).patch("/veiculos/{id}", original.id()),
            404
        );
        assertThat(requisicao().get("/veiculos/{id}", original.id()).as(RespostaVeiculo.class)).isEqualTo(original);
    }

    private RespostaLista<RespostaVeiculo> listar(final Map<String, ?> parametros) {
        final var resposta = requisicao().queryParams(parametros).get("/veiculos");
        assertThat(resposta.statusCode()).isEqualTo(200);
        return resposta.as(new TypeRef<RespostaLista<RespostaVeiculo>>() {});
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
}
