package com.oficina.api.steps;

import com.oficina.api.apresentacao.RespostaLista;
import com.oficina.api.apresentacao.cliente.RespostaCliente;
import com.oficina.api.infraestrutura.seguranca.EmissorTokenJwt;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.common.mapper.TypeRef;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.assertThat;

public class ClientesSteps {
    @Autowired
    private HttpSteps http;

    @Autowired
    private EmissorTokenJwt emissor;

    @Autowired
    private OrdensServicoSteps ordens;

    private @Nullable RespostaCliente clienteOriginal;

    @Before(value = "@clientes")
    public void prepararCenario() {
        this.ordens.limpar();
        this.http.autenticar(this.emissor.emitir("admin", Set.of("ADMIN")).valor());
    }

    @After(value = "@clientes")
    public void limpar() {
        this.ordens.limpar();
    }

    @Given("que o cliente possui uma OS em {string}")
    public void prepararOrdem(final String status) {
        this.ordens
            .prepararStatus(
                "entregue".equals(status)
                    ? "aguardando-retirada"
                    : status
            );
        if ("entregue".equals(status)) {
            this.ordens.prepararRetirada();
        }
        consultarDocumento("52998224725");
        this.http.validarStatus(200);
        this.clienteOriginal = this.http
            .resposta()
            .as(new TypeRef<RespostaLista<RespostaCliente>>() {})
            .itens()
            .getFirst();
    }

    @Given("que o cliente possui outra OS aguardando diagnóstico")
    public void prepararOutraOrdem() {
        this.ordens.criar();
        this.http.validarStatus(201);
    }

    @Given("que o cliente possui uma OS com orçamento rejeitado aguardando retirada")
    public void prepararOrdemRejeitada() {
        prepararOrdem("aguardando-confirmacao");
        this.ordens.prepararDecisao("rejeitado");
    }

    @Then("a OS do cliente deve permanecer em {string}")
    public void validarOrdemPreservada(final String status) {
        this.ordens.consultar();
        this.http.validarStatus(200);
        this.ordens.validarStatusOrdem(status);
        this.ordens.validarClienteOriginal();
    }

    @Given("que existe outro cliente")
    public void cadastrarOutroCliente() {
        cadastrarCliente("Outro cliente", "outro@example.com", "11144477735");
        this.http.validarStatus(201);
    }

    @Given("que existe um cliente")
    public void cadastrarClienteExistente() {
        cadastrarClienteExistente("52998224725");
    }

    @Given("que existe um cliente com os dados:")
    public void cadastrarClienteExistente(final DataTable dados) {
        final var cliente = dados.asMaps().getFirst();
        cadastrarCliente(
            Objects.requireNonNull(cliente.get("nome")),
            Objects.requireNonNull(cliente.get("email")),
            Objects.requireNonNull(cliente.get("documento"))
        );
        this.http.validarStatus(201);
        this.clienteOriginal = this.http.resposta().as(RespostaCliente.class);
    }

    @Given("que existe um cliente com documento {string}")
    public void cadastrarClienteExistente(final String documento) {
        cadastrarCliente("Maria Silva", "maria@example.com", documento);
        this.http.validarStatus(201);
        this.clienteOriginal = this.http.resposta().as(RespostaCliente.class);
    }

    @When("cadastro um cliente com nome {string}, email {string} e documento {string}")
    public void cadastrarCliente(final String nome, final String email, final String documento) {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("nome", nome, "email", email, "documento", documento))
                    .post("/clientes")
            );
    }

    @When("consulto o cliente cadastrado pelo ID")
    public void consultarCliente() {
        consultarCliente(clienteOriginal().id().toString());
    }

    @When("consulto o cliente pelo ID {string}")
    public void consultarCliente(final String id) {
        this.http.receber(this.http.requisicao().get("/clientes/{id}", id));
    }

    @When("consulto clientes pelo documento {string}")
    public void consultarDocumento(final String documento) {
        this.http.receber(this.http.requisicao().queryParam("documento", documento).get("/clientes"));
    }

    @When("atualizo o cliente cadastrado com:")
    public void atualizarCliente(final DataTable dados) {
        enviarAtualizacao(clienteOriginal().id().toString(), dados.asMaps().getFirst());
    }

    @When("atualizo o cliente cadastrado com nome {string}, email {string} e documento {string}")
    public void atualizarCliente(final String nome, final String email, final String documento) {
        enviarAtualizacao(
            clienteOriginal().id().toString(),
            Map.of("nome", nome, "email", email, "documento", documento)
        );
    }

    @When("atualizo o cliente cadastrado sem campos definidos")
    public void atualizarClienteSemCampos() {
        enviarAtualizacao(clienteOriginal().id().toString(), Map.of());
    }

    @When("atualizo o cliente pelo ID {string} com:")
    public void atualizarClientePorId(final String id, final DataTable dados) {
        enviarAtualizacao(id, dados.asMaps().getFirst());
    }

    @When("excluo o cliente cadastrado")
    public void excluirCliente() {
        excluirCliente(clienteOriginal().id().toString());
    }

    @When("excluo o cliente pelo ID {string}")
    public void excluirCliente(final String id) {
        this.http.receber(this.http.requisicao().delete("/clientes/{id}", id));
    }

    @Then("a resposta deve conter o ID do cliente cadastrado")
    public void validarIdCliente() {
        assertThat(this.http.resposta().as(RespostaCliente.class).id()).isEqualTo(clienteOriginal().id());
    }

    @Then("a resposta deve conter o cliente:")
    public void validarCliente(final DataTable tabela) {
        validarDadosCliente(this.http.resposta().as(RespostaCliente.class), tabela);
        assertThat(this.http.resposta().jsonPath().getMap("")).containsOnlyKeys("id", "nome", "email", "documento");
    }

    @Then("a resposta deve conter uma lista com o cliente:")
    public void validarListaClientes(final DataTable tabela) {
        assertThat(this.http.resposta().as(new TypeRef<RespostaLista<RespostaCliente>>() {}).itens())
            .singleElement()
            .satisfies(cliente -> validarDadosCliente(cliente, tabela));
    }

    @Then("a resposta deve conter uma lista vazia de clientes")
    public void validarListaVazia() {
        final var lista = this.http.resposta().as(new TypeRef<RespostaLista<RespostaCliente>>() {});
        assertThat(lista.itens()).isEmpty();
        assertThat(lista.nextCursor()).isNull();
    }

    @Then("o cliente original deve permanecer inalterado")
    public void validarClienteInalterado() {
        final var original = clienteOriginal();
        consultarCliente();
        this.http.validarStatus(200);
        assertThat(this.http.resposta().as(RespostaCliente.class)).isEqualTo(original);
    }

    @Then("o novo cadastro deve ter outro ID")
    public void validarNovoId() {
        assertThat(this.http.resposta().as(RespostaCliente.class).id()).isNotEqualTo(clienteOriginal().id());
    }

    private void enviarAtualizacao(final String id, final Map<?, ?> dados) {
        this.http.receber(this.http.requisicao().body(dados).patch("/clientes/{id}", id));
    }

    private void validarDadosCliente(final RespostaCliente cliente, final DataTable tabela) {
        final var esperado = tabela.asMaps().getFirst();
        assertThat(cliente.id()).isNotNull();
        assertThat(Map.of("nome", cliente.nome(), "email", cliente.email(), "documento", cliente.documento()))
            .containsAllEntriesOf(esperado);
    }

    private RespostaCliente clienteOriginal() {
        return Objects.requireNonNull(this.clienteOriginal);
    }
}
