package com.oficina.api.steps;

import com.oficina.api.apresentacao.RespostaLista;
import com.oficina.api.apresentacao.insumo.RespostaInsumo;
import com.oficina.api.infraestrutura.seguranca.EmissorTokenJwt;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.common.mapper.TypeRef;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

public class InsumosSteps {
    @Autowired
    private HttpSteps http;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EmissorTokenJwt emissor;
    private @Nullable RespostaInsumo insumoOriginal;

    @Before(value = "@insumos")
    public void prepararCenario() {
        this.jdbc.update("DELETE FROM oficina.servicos_insumos_obrigatorios");
        this.jdbc.update("DELETE FROM oficina.insumos");
        this.http.autenticar(this.emissor.emitir("admin", Set.of("ADMIN")).valor());
    }

    @Given("que existe um insumo com nome {string} e preço {string}")
    public void cadastrarExistente(final String nome, final String preco) {
        cadastrarInsumo(nome, preco);
        this.http.validarStatus(201);
        this.insumoOriginal = this.http.resposta().as(RespostaInsumo.class);
    }

    @When("cadastro um insumo com nome {string} e preço {string}")
    public void cadastrarInsumo(final String nome, final String preco) {
        this.http
            .receber(
                this.http.requisicao().body(Map.of("nome", nome, "precoVenda", new BigDecimal(preco))).post("/insumos")
            );
    }

    @When("consulto o insumo cadastrado pelo ID")
    public void consultarInsumo() {
        this.http.receber(this.http.requisicao().get("/insumos/{id}", original().id()));
    }

    @When("consulto insumos pelo nome {string}")
    public void consultarNome(final String nome) {
        this.http.receber(this.http.requisicao().queryParam("nome", nome).get("/insumos"));
    }

    @When("listo os insumos")
    public void listarInsumos() {
        this.http.receber(this.http.requisicao().get("/insumos"));
    }

    @When("atualizo o insumo cadastrado com:")
    public void atualizarInsumo(final DataTable dados) {
        this.http
            .receber(this.http.requisicao().body(dados.asMaps().getFirst()).patch("/insumos/{id}", original().id()));
    }

    @When("excluo o insumo cadastrado")
    public void excluirInsumo() {
        this.http.receber(this.http.requisicao().delete("/insumos/{id}", original().id()));
    }

    @Then("a resposta deve conter o insumo:")
    public void validarInsumo(final DataTable tabela) {
        final var esperado = tabela.asMaps().getFirst();
        final var insumo = this.http.resposta().as(RespostaInsumo.class);
        assertThat(insumo.id()).isNotNull();
        assertThat(insumo.nome()).isEqualTo(esperado.get("nome"));
        assertThat(insumo.precoVenda()).isEqualByComparingTo(esperado.get("precoVenda"));
        assertThat(this.http.resposta().jsonPath().getMap("")).containsOnlyKeys("id", "nome", "precoVenda");
    }

    @Then("a lista de insumos deve conter os nomes:")
    public void validarLista(final DataTable tabela) {
        assertThat(this.http.resposta().as(new TypeRef<RespostaLista<RespostaInsumo>>() {}).itens())
            .extracting(RespostaInsumo::nome)
            .containsExactlyInAnyOrderElementsOf(tabela.asList());
    }

    @Then("a lista de insumos deve estar vazia")
    public void validarListaVazia() {
        assertThat(this.http.resposta().as(new TypeRef<RespostaLista<RespostaInsumo>>() {}).itens()).isEmpty();
    }

    @Then("o insumo original deve permanecer inalterado")
    public void validarInalterado() {
        consultarInsumo();
        this.http.validarStatus(200);
        assertThat(this.http.resposta().as(RespostaInsumo.class)).isEqualTo(original());
    }

    @Then("o novo insumo deve ter outro ID")
    public void validarNovoId() {
        assertThat(this.http.resposta().as(RespostaInsumo.class).id()).isNotEqualTo(original().id());
    }

    private RespostaInsumo original() {
        return Objects.requireNonNull(this.insumoOriginal);
    }
}
