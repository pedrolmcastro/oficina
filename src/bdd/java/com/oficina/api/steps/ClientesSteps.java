package com.oficina.api.steps;

import com.oficina.api.apresentacao.cliente.RespostaCliente;
import com.oficina.api.apresentacao.cliente.RespostaListaClientes;
import com.oficina.api.infraestrutura.seguranca.EmissorTokenJwt;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

public class ClientesSteps {
    @Autowired
    private HttpSteps http;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EmissorTokenJwt emissor;

    private @Nullable RespostaCliente clienteOriginal;

    @Before(value = "@clientes")
    public void prepararCenario() {
        this.jdbc.update("DELETE FROM oficina.veiculos");
        this.jdbc.update("DELETE FROM oficina.clientes");
        this.http.autenticar(this.emissor.emitir("admin", Set.of("ADMIN")).valor());
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
        this.http.receber(this.http.requisicao().get("/clientes/{id}", clienteOriginal().id()));
    }

    @When("consulto clientes pelo documento {string}")
    public void consultarDocumento(final String documento) {
        this.http.receber(this.http.requisicao().queryParam("documento", documento).get("/clientes"));
    }

    @When("atualizo o cliente cadastrado com:")
    public void atualizarCliente(final DataTable dados) {
        this.http
            .receber(
                this.http.requisicao().body(dados.asMaps().getFirst()).patch("/clientes/{id}", clienteOriginal().id())
            );
    }

    @When("excluo o cliente cadastrado")
    public void excluirCliente() {
        this.http.receber(this.http.requisicao().delete("/clientes/{id}", clienteOriginal().id()));
    }

    @Then("a resposta deve conter o cliente:")
    public void validarCliente(final DataTable tabela) {
        validarDadosCliente(this.http.resposta().as(RespostaCliente.class), tabela);
        assertThat(this.http.resposta().jsonPath().getMap("")).containsOnlyKeys("id", "nome", "email", "documento");
    }

    @Then("a resposta deve conter uma lista com o cliente:")
    public void validarListaClientes(final DataTable tabela) {
        assertThat(this.http.resposta().as(RespostaListaClientes.class).clientes())
            .singleElement()
            .satisfies(cliente -> validarDadosCliente(cliente, tabela));
    }

    @Then("a resposta deve conter uma lista vazia de clientes")
    public void validarListaVazia() {
        assertThat(this.http.resposta().as(RespostaListaClientes.class).clientes()).isEmpty();
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

    private void validarDadosCliente(final RespostaCliente cliente, final DataTable tabela) {
        final var esperado = tabela.asMaps().getFirst();
        assertThat(cliente.id()).isNotNull();
        assertThat(cliente.nome()).isEqualTo(esperado.get("nome"));
        assertThat(cliente.email()).isEqualTo(esperado.get("email"));
        assertThat(cliente.documento()).isEqualTo(esperado.get("documento"));
    }

    private RespostaCliente clienteOriginal() {
        return Objects.requireNonNull(this.clienteOriginal);
    }
}
