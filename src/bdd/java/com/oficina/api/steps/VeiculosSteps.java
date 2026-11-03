package com.oficina.api.steps;

import com.oficina.api.apresentacao.RespostaLista;
import com.oficina.api.apresentacao.cliente.RespostaCliente;
import com.oficina.api.apresentacao.veiculo.RespostaVeiculo;
import com.oficina.api.infraestrutura.seguranca.EmissorTokenJwt;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.common.mapper.TypeRef;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

public class VeiculosSteps {
    @Autowired
    private HttpSteps http;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EmissorTokenJwt emissor;
    private @Nullable RespostaVeiculo veiculoOriginal;
    private @Nullable UUID clienteId;
    private @Nullable UUID outroClienteId;
    private final List<RespostaVeiculo> veiculosDoCliente = new ArrayList<>();
    private int tamanhoPagina = 25;

    @Before(value = "@veiculos")
    public void prepararCenario() {
        this.jdbc.update("DELETE FROM oficina.veiculos");
        this.jdbc.update("DELETE FROM oficina.clientes");
        this.http.autenticar(this.emissor.emitir("admin", Set.of("ADMIN")).valor());
        final var resposta = this.http
            .requisicao()
            .body(Map.of("nome", "Maria", "email", "maria@example.com", "documento", "52998224725"))
            .post("/clientes");
        assertThat(resposta.statusCode()).isEqualTo(201);
        this.clienteId = resposta.as(RespostaCliente.class).id();
    }

    @Given("que existe outro veículo")
    public void cadastrarOutroVeiculo() {
        cadastrarVeiculo("DEF1234");
        this.http.validarStatus(201);
    }

    @Given("que existe um veículo")
    public void cadastrarExistente() {
        cadastrarExistente("ABC1234");
    }

    @Given("que existe um veículo com os dados:")
    public void cadastrarExistente(final DataTable dados) {
        final var veiculo = dados.asMaps().getFirst();
        cadastrarVeiculo(
            Objects.requireNonNull(veiculo.get("placa")),
            Objects.requireNonNull(veiculo.get("modelo")),
            Objects.requireNonNull(veiculo.get("marca")),
            Integer.parseInt(Objects.requireNonNull(veiculo.get("ano")))
        );
        this.http.validarStatus(201);
        this.veiculoOriginal = this.http.resposta().as(RespostaVeiculo.class);
    }

    @Given("que existe um veículo com placa {string}")
    public void cadastrarExistente(final String placa) {
        cadastrarVeiculo(placa);
        this.http.validarStatus(201);
        this.veiculoOriginal = this.http.resposta().as(RespostaVeiculo.class);
    }

    @When("cadastro um veículo com placa {string}")
    public void cadastrarVeiculo(final String placa) {
        cadastrarVeiculo(placa, "Corolla", "Toyota", 2024);
    }

    @When("cadastro um veículo com placa {string}, modelo {string}, marca {string} e ano {int}")
    public void cadastrarVeiculo(final String placa, final String modelo, final String marca, final int ano) {
        enviarCadastro(placa, modelo, marca, ano, Objects.requireNonNull(this.clienteId));
    }

    @When("cadastro um veículo com placa {string} para o cliente {string}")
    public void cadastrarVeiculoParaCliente(final String placa, final String cliente) {
        enviarCadastro(placa, "Corolla", "Toyota", 2024, UUID.fromString(cliente));
    }

    @Given("que existe outro cliente para associar ao veículo")
    public void cadastrarOutroCliente() {
        final var resposta = this.http
            .requisicao()
            .body(Map.of("nome", "João", "email", "joao@example.com", "documento", "11144477735"))
            .post("/clientes");
        assertThat(resposta.statusCode()).isEqualTo(201);
        this.outroClienteId = resposta.as(RespostaCliente.class).id();
    }

    @Given("que outro cliente possui {int} veículo(s)")
    public void cadastrarVeiculosDeOutroCliente(final int quantidade) {
        cadastrarOutroCliente();
        for (int indice = 0; indice < quantidade; indice++) {
            enviarCadastro(
                "OUT%04d".formatted(indice),
                "Corolla",
                "Toyota",
                2024,
                Objects.requireNonNull(this.outroClienteId)
            );
            this.http.validarStatus(201);
        }
    }

    @When("atualizo o veículo cadastrado para o outro cliente")
    public void atualizarClienteAssociado() {
        enviarAtualizacao(original().id().toString(), Map.of("clienteId", this.outroClienteId));
    }

    @When("consulto veículos pelo ID do outro cliente")
    public void consultarVeiculosDoOutroCliente() {
        consultarVeiculosDoCliente(Objects.requireNonNull(this.outroClienteId).toString());
    }

    @Then("a resposta deve conter o veículo associado ao outro cliente com os demais dados preservados")
    public void validarClienteAssociado() {
        assertThat(this.http.resposta().as(RespostaVeiculo.class)).isEqualTo(veiculoComOutroCliente());
    }

    @Then("a lista deve conter somente o veículo associado ao outro cliente")
    public void validarListaDoOutroCliente() {
        assertThat(listaVeiculos().itens()).containsExactly(veiculoComOutroCliente());
        assertThat(listaVeiculos().nextCursor()).isNull();
    }

    @Given("que o cliente possui {int} veículo(s)")
    public void cadastrarVeiculosDoCliente(final int quantidade) {
        for (int indice = 0; indice < quantidade; indice++) {
            cadastrarVeiculo("TST%04d".formatted(indice));
            this.http.validarStatus(201);
            this.veiculosDoCliente.add(this.http.resposta().as(RespostaVeiculo.class));
        }
    }

    @When("consulto veículos sem cliente e sem placa")
    public void consultarSemFiltro() {
        this.http.receber(this.http.requisicao().get("/veiculos"));
    }

    @When("consulto veículos pelo ID do cliente cadastrado")
    public void consultarVeiculosDoCliente() {
        consultarVeiculosDoCliente(Objects.requireNonNull(this.clienteId).toString());
    }

    @When("consulto veículos pelo ID do cliente {string}")
    public void consultarVeiculosDoCliente(final String id) {
        this.http.receber(this.http.requisicao().queryParam("cliente", id).get("/veiculos"));
    }

    @When("consulto veículos pelo ID do cliente cadastrado com limite {int}")
    public void consultarVeiculosDoCliente(final int limite) {
        this.tamanhoPagina = limite;
        this.http
            .receber(
                this.http
                    .requisicao()
                    .queryParam("cliente", this.clienteId)
                    .queryParam("first", limite)
                    .get("/veiculos")
            );
    }

    @When("consulto a próxima página de veículos do cliente cadastrado")
    public void consultarProximaPagina() {
        final var cursor = Objects.requireNonNull(listaVeiculos().nextCursor());
        this.http
            .receber(
                this.http
                    .requisicao()
                    .queryParam("cliente", this.clienteId)
                    .queryParam("first", this.tamanhoPagina)
                    .queryParam("after", cursor)
                    .get("/veiculos")
            );
    }

    @When("consulto veículos pelo ID do cliente cadastrado com paginação:")
    public void consultarComPaginacao(final DataTable dados) {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .queryParam("cliente", this.clienteId)
                    .queryParams(dados.asMaps().getFirst())
                    .get("/veiculos")
            );
    }

    @Then("a página deve conter o veículo {int} do cliente em ordem de ID")
    public void validarPagina(final int posicao) {
        final var esperados = this.veiculosDoCliente
            .stream()
            .sorted(Comparator.comparing(veiculo -> veiculo.id().toString()))
            .toList();
        assertThat(listaVeiculos().itens()).containsExactly(esperados.get(posicao - 1));
    }

    @Then("a resposta deve indicar a próxima página de veículos")
    public void validarProximaPagina() {
        final var lista = listaVeiculos();
        assertThat(lista.itens()).isNotEmpty();
        assertThat(lista.nextCursor()).isEqualTo(lista.itens().getLast().id());
    }

    @Then("a resposta deve indicar o fim da lista de veículos")
    public void validarFimDaLista() {
        assertThat(listaVeiculos().nextCursor()).isNull();
    }

    @When("consulto o veículo cadastrado pelo ID")
    public void consultarVeiculo() {
        consultarVeiculo(original().id().toString());
    }

    @When("consulto o veículo pelo ID {string}")
    public void consultarVeiculo(final String id) {
        this.http.receber(this.http.requisicao().get("/veiculos/{id}", id));
    }

    @When("consulto veículos pela placa {string}")
    public void consultarPlaca(final String placa) {
        this.http.receber(this.http.requisicao().queryParam("placa", placa).get("/veiculos"));
    }

    @When("atualizo o veículo cadastrado com:")
    public void atualizarVeiculo(final DataTable dados) {
        enviarAtualizacao(original().id().toString(), dados.asMaps().getFirst());
    }

    @When("atualizo o veículo cadastrado com placa {string}, modelo {string}, marca {string} e ano {int}")
    public void atualizarVeiculo(final String placa, final String modelo, final String marca, final int ano) {
        enviarAtualizacao(
            original().id().toString(),
            Map.of("placa", placa, "modelo", modelo, "marca", marca, "ano", ano)
        );
    }

    @When("atualizo o veículo cadastrado sem campos definidos")
    public void atualizarVeiculoSemCampos() {
        enviarAtualizacao(original().id().toString(), Map.of());
    }

    @When("atualizo o veículo pelo ID {string} com:")
    public void atualizarVeiculoPorId(final String id, final DataTable dados) {
        enviarAtualizacao(id, dados.asMaps().getFirst());
    }

    @When("excluo o veículo cadastrado")
    public void excluirVeiculo() {
        excluirVeiculo(original().id().toString());
    }

    @When("excluo o veículo pelo ID {string}")
    public void excluirVeiculo(final String id) {
        this.http.receber(this.http.requisicao().delete("/veiculos/{id}", id));
    }

    @Then("a resposta deve conter o ID do veículo cadastrado")
    public void validarIdVeiculo() {
        assertThat(this.http.resposta().as(RespostaVeiculo.class).id()).isEqualTo(original().id());
    }

    @Then("a resposta deve conter o veículo:")
    public void validarVeiculo(final DataTable tabela) {
        validarDadosVeiculo(this.http.resposta().as(RespostaVeiculo.class), tabela);
        assertThat(this.http.resposta().jsonPath().getMap(""))
            .containsOnlyKeys("id", "placa", "modelo", "marca", "ano", "clienteId");
    }

    @Then("a resposta deve conter uma lista com o veículo:")
    public void validarLista(final DataTable tabela) {
        assertThat(this.http.resposta().as(new TypeRef<RespostaLista<RespostaVeiculo>>() {}).itens())
            .singleElement()
            .satisfies(veiculo -> validarDadosVeiculo(veiculo, tabela));
    }

    @Then("a resposta deve conter uma lista vazia de veículos")
    public void validarListaVazia() {
        final var lista = listaVeiculos();
        assertThat(lista.itens()).isEmpty();
        assertThat(lista.nextCursor()).isNull();
    }

    @Then("o veículo original deve permanecer inalterado")
    public void validarInalterado() {
        final var original = original();
        consultarVeiculo();
        this.http.validarStatus(200);
        assertThat(this.http.resposta().as(RespostaVeiculo.class)).isEqualTo(original);
    }

    @Then("o novo veículo deve ter outro ID")
    public void validarNovoId() {
        assertThat(this.http.resposta().as(RespostaVeiculo.class).id()).isNotEqualTo(original().id());
    }

    private RespostaVeiculo veiculoComOutroCliente() {
        final var veiculo = original();
        return new RespostaVeiculo(
            veiculo.id(),
            veiculo.placa(),
            veiculo.modelo(),
            veiculo.marca(),
            veiculo.ano(),
            Objects.requireNonNull(this.outroClienteId)
        );
    }

    private void enviarCadastro(
        final String placa,
        final String modelo,
        final String marca,
        final int ano,
        final UUID cliente
    ) {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("placa", placa, "modelo", modelo, "marca", marca, "ano", ano, "clienteId", cliente))
                    .post("/veiculos")
            );
    }

    private void enviarAtualizacao(final String id, final Map<?, ?> dados) {
        this.http.receber(this.http.requisicao().body(dados).patch("/veiculos/{id}", id));
    }

    private void validarDadosVeiculo(final RespostaVeiculo veiculo, final DataTable tabela) {
        final var esperado = tabela.asMaps().getFirst();
        assertThat(veiculo.id()).isNotNull();
        assertThat(
            Map
                .of(
                    "placa",
                    veiculo.placa(),
                    "modelo",
                    veiculo.modelo(),
                    "marca",
                    veiculo.marca(),
                    "ano",
                    Integer.toString(veiculo.ano())
                )
        ).containsAllEntriesOf(esperado);
        assertThat(veiculo.clienteId()).isEqualTo(this.clienteId);
    }

    private RespostaLista<RespostaVeiculo> listaVeiculos() {
        return this.http.resposta().as(new TypeRef<RespostaLista<RespostaVeiculo>>() {});
    }

    private RespostaVeiculo original() {
        return Objects.requireNonNull(this.veiculoOriginal);
    }
}
