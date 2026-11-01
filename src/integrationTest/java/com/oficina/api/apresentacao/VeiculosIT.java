package com.oficina.api.apresentacao;

import com.oficina.api.apresentacao.cliente.RespostaCliente;
import com.oficina.api.apresentacao.veiculo.RespostaVeiculo;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import java.util.Comparator;
import java.util.List;
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
    void deveRespeitarContratoHttpNoCicloDeVidaDoVeiculo() {
        final var cadastro = cadastrar("ABC1234");
        validar(cadastro, 201);
        final var veiculo = cadastro.as(RespostaVeiculo.class);
        assertThat(cadastro.header("Location")).endsWith("/veiculos/" + veiculo.id());
        assertThat(veiculo)
            .isEqualTo(new RespostaVeiculo(veiculo.id(), "ABC1234", "Corolla", "Toyota", 2024, this.clienteId));
        final var consulta = requisicao().get("/veiculos/{id}", veiculo.id());
        validar(consulta, 200);
        assertThat(consulta.as(RespostaVeiculo.class)).isEqualTo(veiculo);
        validar(requisicao().queryParam("placa", veiculo.placa()).get("/veiculos"), 200);
        validar(requisicao().body(Map.of("modelo", "Civic")).patch("/veiculos/{id}", veiculo.id()), 200);
        final var exclusao = requisicao().delete("/veiculos/{id}", veiculo.id());
        validar(exclusao, 204);
        assertThat(exclusao.body().asString()).isEmpty();
    }

    @Test
    void deveAtualizarSomenteClientePreservandoDemaisCampos() {
        final var original = cadastrar("ABC1234").as(RespostaVeiculo.class);
        final var novoCliente = criarCliente("11144477735");
        final var resposta = requisicao().body(Map.of("clienteId", novoCliente)).patch("/veiculos/{id}", original.id());
        validar(resposta, 200);
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
        validar(requisicao().body(Map.of("modelo", "Civic")).request(metodo, "/veiculos/{id}", original.id()), 404);
    }

    @Test
    void deveRetornarListaVaziaParaPlacaExcluida() {
        final var veiculo = cadastrar("ABC1234").as(RespostaVeiculo.class);
        assertThat(requisicao().delete("/veiculos/{id}", veiculo.id()).statusCode()).isEqualTo(204);

        final var resposta = requisicao().queryParam("placa", veiculo.placa()).get("/veiculos");

        validar(resposta, 200);
        assertThat(resposta.as(new TypeRef<RespostaLista<RespostaVeiculo>>() {}).itens()).isEmpty();
        assertThat(listar(Map.of("cliente", this.clienteId)).itens()).isEmpty();
    }

    @Test
    void deveFiltrarPorPlacaClienteEAmbos() {
        final var primeiro = cadastrar("ABC1234").as(RespostaVeiculo.class);
        final var terceiro = cadastrar("GHI5678").as(RespostaVeiculo.class);
        final var outroCliente = criarCliente("11144477735");
        assertThat(cadastrar("DEF1G23", outroCliente).statusCode()).isEqualTo(201);

        final var porPlaca = listar(Map.of("placa", primeiro.placa(), "first", 1));
        assertThat(porPlaca.itens()).containsExactly(primeiro);
        assertThat(porPlaca.nextCursor()).isNull();
        assertThat(listar(Map.of("cliente", this.clienteId)).itens()).containsExactlyInAnyOrder(primeiro, terceiro);
        assertThat(listar(Map.of("placa", primeiro.placa(), "cliente", this.clienteId)).itens())
            .containsExactly(primeiro);
        assertThat(listar(Map.of("placa", primeiro.placa(), "cliente", outroCliente)).itens()).isEmpty();
        assertThat(listar(Map.of("cliente", UUID.randomUUID())).itens()).isEmpty();
    }

    @Test
    void devePercorrerTodosOsVeiculosEmOrdemSemRepeticoes() {
        final var esperados = List
            .of(
                cadastrar("ABC1234").as(RespostaVeiculo.class),
                cadastrar("DEF1G23").as(RespostaVeiculo.class),
                cadastrar("GHI5678").as(RespostaVeiculo.class)
            )
            .stream()
            .sorted(Comparator.comparing(veiculo -> veiculo.id().toString()))
            .toList();

        final var primeira = listar(Map.of("cliente", this.clienteId, "first", 2));
        assertThat(primeira.itens()).containsExactlyElementsOf(esperados.subList(0, 2));
        assertThat(primeira.nextCursor()).isEqualTo(esperados.get(1).id());

        final var ultima = listar(Map.of("cliente", this.clienteId, "first", 2, "after", primeira.nextCursor()));
        assertThat(ultima.itens()).containsExactly(esperados.getLast());
        assertThat(ultima.nextCursor()).isNull();
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
        return cadastrar(placa, this.clienteId);
    }

    private Response cadastrar(final String placa, final UUID cliente) {
        return requisicao()
            .body(Map.of("placa", placa, "modelo", "Corolla", "marca", "Toyota", "ano", 2024, "clienteId", cliente))
            .post("/veiculos");
    }
}
