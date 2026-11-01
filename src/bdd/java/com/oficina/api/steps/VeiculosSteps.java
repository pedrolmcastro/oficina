package com.oficina.api.steps;

import com.oficina.api.apresentacao.cliente.RespostaCliente;
import com.oficina.api.apresentacao.veiculo.RespostaVeiculo;
import com.oficina.api.infraestrutura.seguranca.EmissorTokenJwt;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
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

    @Given("que existe um veículo com placa {string}")
    public void cadastrarExistente(final String placa) {
        cadastrarVeiculo(placa);
        this.http.validarStatus(201);
        this.veiculoOriginal = this.http.resposta().as(RespostaVeiculo.class);
    }

    @When("cadastro um veículo com placa {string}")
    public void cadastrarVeiculo(final String placa) {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(
                        Map
                            .of(
                                "placa",
                                placa,
                                "modelo",
                                "Corolla",
                                "marca",
                                "Toyota",
                                "ano",
                                2024,
                                "clienteId",
                                this.clienteId
                            )
                    )
                    .post("/veiculos")
            );
    }

    @When("consulto o veículo cadastrado pelo ID")
    public void consultarVeiculo() {
        this.http.receber(this.http.requisicao().get("/veiculos/{id}", original().id()));
    }

    @When("consulto veículos pela placa {string}")
    public void consultarPlaca(final String placa) {
        this.http.receber(this.http.requisicao().queryParam("placa", placa).get("/veiculos"));
    }

    @When("atualizo o veículo cadastrado com:")
    public void atualizarVeiculo(final DataTable dados) {
        this.http
            .receber(this.http.requisicao().body(dados.asMaps().getFirst()).patch("/veiculos/{id}", original().id()));
    }

    @When("excluo o veículo cadastrado")
    public void excluirVeiculo() {
        this.http.receber(this.http.requisicao().delete("/veiculos/{id}", original().id()));
    }

    @Then("a resposta deve conter o veículo:")
    public void validarVeiculo(final DataTable tabela) {
        final var esperado = tabela.asMaps().getFirst();
        final var veiculo = this.http.resposta().as(RespostaVeiculo.class);
        assertThat(veiculo.id()).isNotNull();
        assertThat(veiculo.placa()).isEqualTo(esperado.get("placa"));
        assertThat(veiculo.modelo()).isEqualTo(esperado.get("modelo"));
        assertThat(veiculo.marca()).isEqualTo(esperado.get("marca"));
        assertThat(veiculo.ano()).isEqualTo(Integer.parseInt(Objects.requireNonNull(esperado.get("ano"))));
        assertThat(veiculo.clienteId()).isEqualTo(this.clienteId);
        assertThat(this.http.resposta().jsonPath().getMap(""))
            .containsOnlyKeys("id", "placa", "modelo", "marca", "ano", "clienteId");
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

    private RespostaVeiculo original() {
        return Objects.requireNonNull(this.veiculoOriginal);
    }
}
