package com.oficina.api.steps;

import com.oficina.api.apresentacao.RespostaLista;
import com.oficina.api.apresentacao.insumo.RespostaInsumo;
import com.oficina.api.apresentacao.servico.RespostaInsumoObrigatorio;
import com.oficina.api.apresentacao.servico.RespostaServico;
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

public class ServicosSteps {
    @Autowired
    private HttpSteps http;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EmissorTokenJwt emissor;
    private @Nullable RespostaServico servicoOriginal;
    private @Nullable RespostaInsumo insumoDisponivel;

    @Before(value = "@servicos")
    public void prepararCenario() {
        this.jdbc.update("DELETE FROM oficina.servicos_insumos_obrigatorios");
        this.jdbc.update("DELETE FROM oficina.servicos");
        this.http.autenticar(this.emissor.emitir("admin", Set.of("ADMIN")).valor());
    }

    @Given("que existe um serviço com nome {string} e preço {string}")
    public void cadastrarExistente(final String nome, final String preco) {
        cadastrarServico(nome, preco);
        this.http.validarStatus(201);
        this.servicoOriginal = this.http.resposta().as(RespostaServico.class);
    }

    @When("cadastro um serviço com nome {string} e preço {string}")
    public void cadastrarServico(final String nome, final String preco) {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("nome", nome, "precoVenda", new BigDecimal(preco), "requerDiagnostico", true))
                    .post("/servicos")
            );
    }

    @When("consulto o serviço cadastrado pelo ID")
    public void consultarServico() {
        this.http.receber(this.http.requisicao().get("/servicos/{id}", original().id()));
    }

    @When("consulto serviços pelo nome {string}")
    public void consultarNome(final String nome) {
        this.http.receber(this.http.requisicao().queryParam("nome", nome).get("/servicos"));
    }

    @When("listo os serviços")
    public void listarServicos() {
        this.http.receber(this.http.requisicao().get("/servicos"));
    }

    @When("atualizo o serviço cadastrado com:")
    public void atualizarServico(final DataTable dados) {
        this.http
            .receber(this.http.requisicao().body(dados.asMaps().getFirst()).patch("/servicos/{id}", original().id()));
    }

    @When("excluo o serviço cadastrado")
    public void excluirServico() {
        this.http.receber(this.http.requisicao().delete("/servicos/{id}", original().id()));
    }

    @Then("a resposta deve conter o serviço:")
    public void validarServico(final DataTable tabela) {
        final var esperado = tabela.asMaps().getFirst();
        final var servico = this.http.resposta().as(RespostaServico.class);
        assertThat(servico.id()).isNotNull();
        assertThat(servico.requerDiagnostico())
            .isEqualTo(Boolean.parseBoolean(esperado.getOrDefault("requerDiagnostico", "true")));
        assertThat(servico.nome()).isEqualTo(esperado.get("nome"));
        assertThat(servico.precoVenda()).isEqualByComparingTo(esperado.get("precoVenda"));
        assertThat(this.http.resposta().jsonPath().getMap(""))
            .containsOnlyKeys("id", "nome", "precoVenda", "requerDiagnostico");
    }

    @Then("a lista de serviços deve conter os nomes:")
    public void validarLista(final DataTable tabela) {
        assertThat(this.http.resposta().as(new TypeRef<RespostaLista<RespostaServico>>() {}).itens())
            .extracting(RespostaServico::nome)
            .containsExactlyInAnyOrderElementsOf(tabela.asList());
    }

    @Then("a lista de serviços deve estar vazia")
    public void validarListaVazia() {
        assertThat(this.http.resposta().as(new TypeRef<RespostaLista<RespostaServico>>() {}).itens()).isEmpty();
    }

    @Then("o serviço original deve permanecer inalterado")
    public void validarInalterado() {
        consultarServico();
        this.http.validarStatus(200);
        assertThat(this.http.resposta().as(RespostaServico.class)).isEqualTo(original());
    }

    @Then("o novo serviço deve ter outro ID")
    public void validarNovoId() {
        assertThat(this.http.resposta().as(RespostaServico.class).id()).isNotEqualTo(original().id());
    }

    @Given("que existe um insumo disponível para o serviço com nome {string} e preço {string}")
    public void cadastrarInsumoDisponivel(final String nome, final String preco) {
        this.jdbc.update("DELETE FROM oficina.insumos");
        this.http
            .receber(
                this.http.requisicao().body(Map.of("nome", nome, "precoVenda", new BigDecimal(preco))).post("/insumos")
            );
        this.http.validarStatus(201);
        this.insumoDisponivel = this.http.resposta().as(RespostaInsumo.class);
    }

    @When("associo o insumo disponível como obrigatório ao serviço cadastrado com quantidade {string}")
    public void associarInsumoObrigatorio(final String quantidade) {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("insumoId", insumo().id(), "quantidade", Integer.parseInt(quantidade)))
                    .post("/servicos/{id}/insumos-obrigatorios", original().id())
            );
    }

    @When("listo os insumos obrigatórios do serviço cadastrado")
    public void listarInsumosObrigatorios() {
        this.http.receber(this.http.requisicao().get("/servicos/{id}/insumos-obrigatorios", original().id()));
    }

    @When("removo o insumo obrigatório do serviço cadastrado")
    public void removerInsumoObrigatorio() {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .delete("/servicos/{id}/insumos-obrigatorios/{insumo}", original().id(), insumo().id())
            );
    }

    @Then("o serviço deve estar sem insumos obrigatórios")
    public void validarSemInsumos() {
        assertThat(this.http.resposta().as(new TypeRef<RespostaLista<RespostaInsumoObrigatorio>>() {}).itens())
            .isEmpty();
    }

    @Then("o serviço deve ter o insumo disponível como obrigatório com quantidade {string}")
    public void validarInsumoObrigatorio(final String quantidade) {
        assertThat(this.http.resposta().as(new TypeRef<RespostaLista<RespostaInsumoObrigatorio>>() {}).itens())
            .containsExactly(new RespostaInsumoObrigatorio(insumo().id(), Integer.parseInt(quantidade)));
    }

    @Then("o insumo disponível deve continuar cadastrado")
    public void validarInsumoPreservado() {
        this.http.receber(this.http.requisicao().get("/insumos/{id}", insumo().id()));
        this.http.validarStatus(200);
        assertThat(this.http.resposta().as(RespostaInsumo.class)).isEqualTo(insumo());
    }

    private RespostaInsumo insumo() {
        return Objects.requireNonNull(this.insumoDisponivel);
    }

    private RespostaServico original() {
        return Objects.requireNonNull(this.servicoOriginal);
    }
}
