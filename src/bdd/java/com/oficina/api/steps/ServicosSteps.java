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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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

    private final List<RespostaServico> servicosCadastrados = new ArrayList<>();
    private int tamanhoPagina = 25;

    @Before(value = "@servicos")
    public void prepararCenario() {
        this.jdbc.update("DELETE FROM oficina.servicos_insumos_obrigatorios");
        this.jdbc.update("DELETE FROM oficina.servicos");
        this.http.autenticar(this.emissor.emitir("admin", Set.of("ADMIN")).valor());
    }

    @Given("que existem {int} serviços")
    public void cadastrarServicos(final int quantidade) {
        for (int indice = 0; indice < quantidade; indice++) {
            cadastrarServico("Serviço " + indice, "19.90");
            this.http.validarStatus(201);
            this.servicosCadastrados.add(this.http.resposta().as(RespostaServico.class));
        }
    }

    @Given("que existe outro serviço")
    public void cadastrarOutroServico() {
        cadastrarServico("Pneu", "100.00");
        this.http.validarStatus(201);
    }

    @Given("que existe um serviço")
    public void cadastrarExistente() {
        cadastrarExistente("Óleo", "19.90");
    }

    @Given("que existe um serviço com nome {string}")
    public void cadastrarExistente(final String nome) {
        cadastrarExistente(nome, "19.90");
    }

    @Given("que existe um serviço com nome {string} e preço {string}")
    public void cadastrarExistente(final String nome, final String preco) {
        cadastrarServico(nome, preco);
        this.http.validarStatus(201);
        this.servicoOriginal = this.http.resposta().as(RespostaServico.class);
    }

    @When("cadastro um serviço com nome {string}")
    public void cadastrarServico(final String nome) {
        cadastrarServico(nome, "19.90");
    }

    @When("cadastro um serviço com nome {string} e preço {string}")
    public void cadastrarServico(final String nome, final String preco) {
        cadastrarServico(nome, preco, "true");
    }

    @When("cadastro um serviço com nome {string}, preço {string} e requer diagnóstico {string}")
    public void cadastrarServico(final String nome, final String preco, final String diagnostico) {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(
                        Map
                            .of(
                                "nome",
                                nome,
                                "precoVenda",
                                new BigDecimal(preco),
                                "requerDiagnostico",
                                Boolean.parseBoolean(diagnostico)
                            )
                    )
                    .post("/servicos")
            );
    }

    @When("consulto o serviço cadastrado pelo ID")
    public void consultarServico() {
        consultarServico(original().id().toString());
    }

    @When("consulto o serviço pelo ID {string}")
    public void consultarServico(final String id) {
        this.http.receber(this.http.requisicao().get("/servicos/{id}", id));
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
        atualizarServicoPorId(original().id().toString(), dados);
    }

    @When("atualizo o serviço cadastrado com nome {string} e preço {string}")
    public void atualizarComNomeEPreco(final String nome, final String preco) {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("nome", nome, "precoVenda", new BigDecimal(preco)))
                    .patch("/servicos/{id}", original().id())
            );
    }

    @When("atualizo o serviço cadastrado sem campos definidos")
    public void atualizarServicoSemCampos() {
        this.http.receber(this.http.requisicao().body(Map.of()).patch("/servicos/{id}", original().id()));
    }

    @When("atualizo o serviço pelo ID {string} com:")
    public void atualizarServicoPorId(final String id, final DataTable dados) {
        this.http.receber(this.http.requisicao().body(dados.asMaps().getFirst()).patch("/servicos/{id}", id));
    }

    @When("excluo o serviço pelo ID {string}")
    public void excluirServico(final String id) {
        this.http.receber(this.http.requisicao().delete("/servicos/{id}", id));
    }

    @When("excluo o serviço cadastrado")
    public void excluirServico() {
        excluirServico(original().id().toString());
    }

    @Then("a resposta deve conter o serviço:")
    public void validarServico(final DataTable tabela) {
        validarDadosServico(this.http.resposta().as(RespostaServico.class), tabela);
        assertThat(this.http.resposta().jsonPath().getMap(""))
            .containsOnlyKeys("id", "nome", "precoVenda", "requerDiagnostico");
    }

    @Then("a lista de serviços deve conter os nomes:")
    public void validarLista(final DataTable tabela) {
        final var nomes = tabela.asList(String.class).stream().filter(Objects::nonNull).toList();
        assertThat(listaServicos().itens())
            .extracting(RespostaServico::nome)
            .containsExactlyInAnyOrderElementsOf(nomes);
    }

    @Then("a lista de serviços deve estar vazia")
    public void validarListaVazia() {
        assertThat(listaServicos().itens()).isEmpty();
        assertThat(listaServicos().nextCursor()).isNull();
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

    @Then("a resposta deve conter o ID do serviço cadastrado")
    public void validarId() {
        assertThat(this.http.resposta().as(RespostaServico.class).id()).isEqualTo(original().id());
    }

    @Then("a resposta deve conter uma lista com o serviço:")
    public void validarListaComServico(final DataTable tabela) {
        assertThat(listaServicos().itens()).singleElement().satisfies(servico -> validarDadosServico(servico, tabela));
        assertThat(listaServicos().nextCursor()).isNull();
    }

    @When("listo os serviços com limite {int}")
    public void listarComLimite(final int limite) {
        this.tamanhoPagina = limite;
        consultarPagina(null);
    }

    @When("consulto a próxima página de serviços")
    public void consultarProximaPagina() {
        consultarPagina(Objects.requireNonNull(listaServicos().nextCursor()));
    }

    @When("listo os serviços com paginação:")
    public void consultarComPaginacao(final DataTable dados) {
        this.http.receber(this.http.requisicao().queryParams(dados.asMaps().getFirst()).get("/servicos"));
    }

    @Then("a página deve conter o serviço {int} em ordem de ID")
    public void validarPagina(final int posicao) {
        assertThat(listaServicos().itens()).containsExactly(servicosEmOrdem().get(posicao - 1));
    }

    @Then("a resposta deve indicar a próxima página de serviços")
    public void validarProximaPagina() {
        final var lista = listaServicos();
        assertThat(lista.itens()).isNotEmpty();
        assertThat(lista.nextCursor()).isEqualTo(lista.itens().getLast().id());
    }

    @Then("a resposta deve indicar o fim da lista de serviços")
    public void validarFimLista() {
        assertThat(listaServicos().nextCursor()).isNull();
    }

    @Given("que existe um insumo disponível para o serviço")
    public void cadastrarInsumoDisponivel() {
        this.jdbc.update("DELETE FROM oficina.insumos");
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("nome", "Óleo", "precoVenda", new BigDecimal("19.90")))
                    .post("/insumos")
            );
        this.http.validarStatus(201);
        this.insumoDisponivel = this.http.resposta().as(RespostaInsumo.class);
    }

    @Given("que o insumo disponível é obrigatório para o serviço cadastrado")
    public void prepararInsumoObrigatorio() {
        prepararInsumoObrigatorio("4");
    }

    @Given("que o insumo disponível é obrigatório para o serviço cadastrado com quantidade {string}")
    public void prepararInsumoObrigatorio(final String quantidade) {
        associarInsumoObrigatorio(quantidade);
        this.http.validarStatus(201);
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

    @When("associo um insumo como obrigatório ao serviço cadastrado com:")
    public void associarInsumoObrigatorio(final DataTable dados) {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(dados.asMaps().getFirst())
                    .post("/servicos/{id}/insumos-obrigatorios", original().id())
            );
    }

    @When("atualizo a associação do insumo obrigatório do serviço cadastrado com:")
    public void atualizarInsumoObrigatorio(final DataTable dados) {
        atualizarInsumoObrigatorioPorId(insumo().id().toString(), dados);
    }

    @When("atualizo a associação do insumo obrigatório pelo ID {string} no serviço cadastrado com:")
    public void atualizarInsumoObrigatorioPorId(final String id, final DataTable dados) {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(dados.asMaps().getFirst())
                    .patch("/servicos/{id}/insumos-obrigatorios/{insumo}", original().id(), id)
            );
    }

    @When("listo os insumos obrigatórios do serviço cadastrado")
    public void listarInsumosObrigatorios() {
        this.http.receber(this.http.requisicao().get("/servicos/{id}/insumos-obrigatorios", original().id()));
    }

    @When("excluo a associação do insumo obrigatório do serviço cadastrado")
    public void excluirInsumoObrigatorio() {
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

    private void consultarPagina(final @Nullable UUID cursor) {
        final Map<String, Object> parametros = new HashMap<>();
        parametros.put("first", this.tamanhoPagina);
        if (cursor != null) {
            parametros.put("after", cursor);
        }
        this.http.receber(this.http.requisicao().queryParams(parametros).get("/servicos"));
    }

    private List<RespostaServico> servicosEmOrdem() {
        return this.servicosCadastrados
            .stream()
            .sorted(Comparator.comparing(servico -> servico.id().toString()))
            .toList();
    }

    private void validarDadosServico(final RespostaServico servico, final DataTable tabela) {
        final var esperado = tabela.asMaps().getFirst();
        assertThat(servico.id()).isNotNull();
        if (esperado.containsKey("nome")) {
            assertThat(servico.nome()).isEqualTo(esperado.get("nome"));
        }
        if (esperado.containsKey("requerDiagnostico")) {
            assertThat(servico.requerDiagnostico()).isEqualTo(Boolean.parseBoolean(esperado.get("requerDiagnostico")));
        }
        if (esperado.containsKey("precoVenda")) {
            assertThat(servico.precoVenda()).isEqualByComparingTo(esperado.get("precoVenda"));
        }
    }

    private RespostaLista<RespostaServico> listaServicos() {
        return this.http.resposta().as(new TypeRef<RespostaLista<RespostaServico>>() {});
    }

    private RespostaServico original() {
        return Objects.requireNonNull(this.servicoOriginal);
    }
}
