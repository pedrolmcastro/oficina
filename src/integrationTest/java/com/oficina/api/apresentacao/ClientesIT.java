package com.oficina.api.apresentacao;

import com.oficina.api.apresentacao.cliente.RespostaCliente;
import io.restassured.common.mapper.TypeRef;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

class ClientesIT extends ApiIntegrationTest {
    @BeforeEach
    void limparClientes() {
        this.jdbc.update("DELETE FROM oficina.veiculos");
        this.jdbc.update("DELETE FROM oficina.clientes");
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
    void deveRetornarListaVaziaParaDocumentoExcluido() {
        final var cliente = criarCliente();
        assertThat(requisicao().delete("/clientes/{id}", cliente.id()).statusCode()).isEqualTo(204);

        final var resposta = requisicao().queryParam("documento", cliente.documento()).get("/clientes");

        validar(resposta, 200);
        assertThat(resposta.as(new TypeRef<RespostaLista<RespostaCliente>>() {}).itens()).isEmpty();
    }

    @Test
    void deveRespeitarContratoHttpNoCicloDeVidaDoCliente() {
        final var cadastro = requisicao()
            .body(Map.of("nome", "Maria Silva", "email", "maria@example.com", "documento", "52998224725"))
            .post("/clientes");
        validar(cadastro, 201);
        final var cliente = cadastro.as(RespostaCliente.class);
        assertThat(cadastro.header("Location")).endsWith("/clientes/" + cliente.id());
        assertThat(cliente)
            .isEqualTo(new RespostaCliente(cliente.id(), "Maria Silva", "maria@example.com", "52998224725"));
        final var consulta = requisicao().get("/clientes/{id}", cliente.id());
        validar(consulta, 200);
        assertThat(consulta.as(RespostaCliente.class)).isEqualTo(cliente);
        validar(requisicao().queryParam("documento", cliente.documento()).get("/clientes"), 200);
        validar(requisicao().body(Map.of("nome", "Novo nome")).patch("/clientes/{id}", cliente.id()), 200);
        final var exclusao = requisicao().delete("/clientes/{id}", cliente.id());
        validar(exclusao, 204);
        assertThat(exclusao.body().asString()).isEmpty();
    }

    private RespostaCliente criarCliente() {
        final var resposta = requisicao()
            .body(Map.of("nome", "Maria Silva", "email", "maria@example.com", "documento", "52998224725"))
            .post("/clientes");
        assertThat(resposta.statusCode()).isEqualTo(201);
        return resposta.as(RespostaCliente.class);
    }
}
