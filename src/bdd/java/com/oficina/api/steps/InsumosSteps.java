package com.oficina.api.steps;

import com.oficina.api.apresentacao.RespostaLista;
import com.oficina.api.apresentacao.cliente.RespostaCliente;
import com.oficina.api.apresentacao.insumo.RespostaInsumo;
import com.oficina.api.apresentacao.ordemservico.RespostaAcessoOrdemServico;
import com.oficina.api.apresentacao.servico.RespostaServico;
import com.oficina.api.apresentacao.veiculo.RespostaVeiculo;
import com.oficina.api.infraestrutura.email.EnvioEmails;
import com.oficina.api.infraestrutura.seguranca.EmissorTokenJwt;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

public class InsumosSteps {
    @Autowired
    private HttpSteps http;
    @Autowired
    private EnvioEmails emails;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EmissorTokenJwt emissor;
    private @Nullable RespostaInsumo insumoOriginal;

    private final List<RespostaInsumo> insumosCadastrados = new ArrayList<>();
    private @Nullable RespostaAcessoOrdemServico acesso;
    private @Nullable UUID servicoId;
    private int tamanhoPagina = 25;

    @Before(value = "@insumos")
    public void prepararCenario() {
        limpar();
        this.http.autenticar(this.emissor.emitir("admin", Set.of("ADMIN")).valor());
    }

    @After(value = "@insumos")
    public void limpar() {
        for (final var tabela : List
            .of(
                "ordens_servico_insumos",
                "ordens_servico_servicos",
                "ordens_servico_eventos",
                "ordens_servico",
                "servicos_insumos_obrigatorios",
                "servicos",
                "insumos",
                "veiculos",
                "clientes"
            )) {
            this.jdbc.update("DELETE FROM oficina." + tabela);
        }
    }

    @Given("que existem {int} insumos")
    public void cadastrarInsumos(final int quantidade) {
        for (int indice = 0; indice < quantidade; indice++) {
            cadastrarExistente("Insumo " + indice, "19.90");
        }
    }

    @Given("que existe outro insumo")
    public void cadastrarOutroInsumo() {
        cadastrarInsumo("Pneu", "100.00");
        this.http.validarStatus(201);
    }

    @Given("que existe um insumo")
    public void cadastrarExistente() {
        cadastrarExistente("Óleo", "19.90");
    }

    @Given("que existe um insumo com nome {string}")
    public void cadastrarExistente(final String nome) {
        cadastrarExistente(nome, "19.90");
    }

    @Given("que existe um insumo com nome {string} e preço {string}")
    public void cadastrarExistente(final String nome, final String preco) {
        cadastrarInsumo(nome, preco);
        this.http.validarStatus(201);
        this.insumoOriginal = this.http.resposta().as(RespostaInsumo.class);
        this.insumosCadastrados.add(original());
    }

    @When("cadastro um insumo com nome {string}")
    public void cadastrarInsumo(final String nome) {
        cadastrarInsumo(nome, "19.90");
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
        consultarInsumo(original().id().toString());
    }

    @When("consulto o insumo pelo ID {string}")
    public void consultarInsumo(final String id) {
        this.http.receber(this.http.requisicao().get("/insumos/{id}", id));
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
        atualizarInsumoPorId(original().id().toString(), dados);
    }

    @When("atualizo o insumo cadastrado com nome {string} e preço {string}")
    public void atualizarComNomeEPreco(final String nome, final String preco) {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("nome", nome, "precoVenda", new BigDecimal(preco)))
                    .patch("/insumos/{id}", original().id())
            );
    }

    @When("atualizo o insumo cadastrado sem campos definidos")
    public void atualizarInsumoSemCampos() {
        this.http.receber(this.http.requisicao().body(Map.of()).patch("/insumos/{id}", original().id()));
    }

    @When("atualizo o insumo pelo ID {string} com:")
    public void atualizarInsumoPorId(final String id, final DataTable dados) {
        this.http.receber(this.http.requisicao().body(dados.asMaps().getFirst()).patch("/insumos/{id}", id));
    }

    @When("excluo o insumo pelo ID {string}")
    public void excluirInsumo(final String id) {
        this.http.receber(this.http.requisicao().delete("/insumos/{id}", id));
    }

    @When("excluo o insumo cadastrado")
    public void excluirInsumo() {
        excluirInsumo(original().id().toString());
    }

    @Then("a resposta deve conter o insumo:")
    public void validarInsumo(final DataTable tabela) {
        validarDadosInsumo(this.http.resposta().as(RespostaInsumo.class), tabela);
        assertThat(this.http.resposta().jsonPath().getMap(""))
            .containsOnlyKeys(
                "id",
                "nome",
                "precoVenda",
                "quantidadeEmEstoque",
                "quantidadeReservada",
                "quantidadeDisponivel"
            );
    }

    @Then("a lista de insumos deve conter os nomes:")
    public void validarLista(final DataTable tabela) {
        final var nomes = tabela.asList(String.class).stream().filter(Objects::nonNull).toList();
        assertThat(listaInsumos().itens()).extracting(RespostaInsumo::nome).containsExactlyInAnyOrderElementsOf(nomes);
    }

    @Then("a lista de insumos deve estar vazia")
    public void validarListaVazia() {
        assertThat(listaInsumos().itens()).isEmpty();
        assertThat(listaInsumos().nextCursor()).isNull();
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

    @Then("a resposta deve conter o ID do insumo cadastrado")
    public void validarId() {
        assertThat(this.http.resposta().as(RespostaInsumo.class).id()).isEqualTo(original().id());
    }

    @Then("a resposta deve conter uma lista com o insumo:")
    public void validarListaComInsumo(final DataTable tabela) {
        assertThat(listaInsumos().itens()).singleElement().satisfies(insumo -> validarDadosInsumo(insumo, tabela));
        assertThat(listaInsumos().nextCursor()).isNull();
    }

    @When("listo os insumos com limite {int}")
    public void listarComLimite(final int limite) {
        this.tamanhoPagina = limite;
        consultarPagina(null);
    }

    @When("consulto a próxima página de insumos")
    public void consultarProximaPagina() {
        consultarPagina(Objects.requireNonNull(listaInsumos().nextCursor()));
    }

    @When("listo os insumos com paginação:")
    public void consultarComPaginacao(final DataTable dados) {
        this.http.receber(this.http.requisicao().queryParams(dados.asMaps().getFirst()).get("/insumos"));
    }

    @Then("a página deve conter o insumo {int} em ordem de ID")
    public void validarPagina(final int posicao) {
        assertThat(listaInsumos().itens()).containsExactly(insumosEmOrdem().get(posicao - 1));
    }

    @Then("a resposta deve indicar a próxima página de insumos")
    public void validarProximaPagina() {
        final var lista = listaInsumos();
        assertThat(lista.itens()).isNotEmpty();
        assertThat(lista.nextCursor()).isEqualTo(lista.itens().getLast().id());
    }

    @Then("a resposta deve indicar o fim da lista de insumos")
    public void validarFimLista() {
        assertThat(listaInsumos().nextCursor()).isNull();
    }

    @When("adiciono {int} unidades ao estoque do insumo cadastrado")
    public void adicionarEstoque(final int quantidade) {
        enviarEntradaEstoque(original().id().toString(), quantidade);
    }

    @When("adiciono {int} unidades ao estoque do insumo pelo ID {string}")
    public void adicionarEstoquePorId(final int quantidade, final String id) {
        enviarEntradaEstoque(id, quantidade);
    }

    @Then("o estoque do insumo deve ter {int} unidades físicas, {int} reservadas e {int} disponíveis")
    public void validarEstoque(final int fisico, final int reservado, final int disponivel) {
        final var insumo = this.http.resposta().as(RespostaInsumo.class);
        assertThat(insumo.quantidadeEmEstoque()).isEqualTo(fisico);
        assertThat(insumo.quantidadeReservada()).isEqualTo(reservado);
        assertThat(insumo.quantidadeDisponivel()).isEqualTo(disponivel);
    }

    @Given("que o insumo é obrigatório para um serviço ativo")
    public void associarServico() {
        associarServico(false);
    }

    private void associarServico(final boolean requerDiagnostico) {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("nome", "Troca de óleo", "precoVenda", 100, "requerDiagnostico", requerDiagnostico))
                    .post("/servicos")
            );
        this.http.validarStatus(201);
        this.servicoId = this.http.resposta().as(RespostaServico.class).id();
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("insumoId", original().id(), "quantidade", 4))
                    .post("/servicos/{id}/insumos-obrigatorios", this.servicoId)
            );
        this.http.validarStatus(201);
    }

    @Given("que o insumo é necessário para uma OS em {string}")
    public void prepararOrdem(final String status) {
        associarServico("aguardando-diagnostico".equals(status) || "em-diagnostico".equals(status));
        criarOrdem();
        this.http
            .receber(
                this.http
                    .requisicao()
                    .delete("/servicos/{id}/insumos-obrigatorios/{insumo}", this.servicoId, original().id())
            );
        this.http.validarStatus(204);
        assertThat(status)
            .isIn(
                "aguardando-diagnostico",
                "em-diagnostico",
                "aguardando-confirmacao",
                "aguardando-insumos",
                "aguardando-execucao",
                "em-execucao"
            );
        final var ordemId = Objects.requireNonNull(this.acesso).ordemServico().id();
        if ("em-diagnostico".equals(status)) {
            this.http.receber(this.http.requisicao().post("/ordens-servico/{id}/inicio-diagnostico", ordemId));
            this.http.validarStatus(200);
        } else if ("aguardando-insumos".equals(status)
            || "aguardando-execucao".equals(status)
            || "em-execucao".equals(status)) {
            aprovarOrdem();
        }
        if ("aguardando-execucao".equals(status) || "em-execucao".equals(status)) {
            adicionarEstoque(4);
            this.http.validarStatus(200);
        }
        if ("em-execucao".equals(status)) {
            this.http.receber(this.http.requisicao().post("/ordens-servico/{id}/inicio-execucao", ordemId));
            this.http.validarStatus(200);
        }
        final var resposta = this.http.requisicao().get("/ordens-servico/{id}", ordemId);
        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.jsonPath().getString("status")).isEqualTo(status);
        consultarInsumo();
        this.http.validarStatus(200);
        this.insumoOriginal = this.http.resposta().as(RespostaInsumo.class);
    }

    private void criarOrdem() {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("nome", "Maria", "email", "maria@example.com", "documento", "52998224725"))
                    .post("/clientes")
            );
        this.http.validarStatus(201);
        final var cliente = this.http.resposta().as(RespostaCliente.class).id();
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(
                        Map
                            .of(
                                "placa",
                                "ABC1234",
                                "modelo",
                                "Corolla",
                                "marca",
                                "Toyota",
                                "ano",
                                2024,
                                "clienteId",
                                cliente
                            )
                    )
                    .post("/veiculos")
            );
        this.http.validarStatus(201);
        final var veiculoId = this.http.resposta().as(RespostaVeiculo.class).id();
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("veiculoId", veiculoId, "servicoIds", List.of(Objects.requireNonNull(this.servicoId))))
                    .post("/ordens-servico")
            );
        this.http.validarStatus(201);
        this.acesso = this.http.resposta().as(RespostaAcessoOrdemServico.class);
    }

    private void aprovarOrdem() {
        final var corpos = ArgumentCaptor.forClass(String.class);
        verify(this.emails, atLeastOnce())
            .enviar(eq(Objects.requireNonNull(this.acesso).ordemServico().id()), anyString(), corpos.capture());
        final var token = corpos
            .getAllValues()
            .stream()
            .filter(corpo -> corpo.contains("/aprovacao"))
            .reduce((_, ultimo) -> ultimo)
            .orElseThrow()
            .split("/", -1)[4];
        this.http
            .receber(
                this.http.requisicao().auth().none().post("/ordens-servico/acompanhamento/{token}/aprovacao", token)
            );
        this.http.validarStatus(200);
    }

    private void enviarEntradaEstoque(final String id, final int quantidade) {
        this.http
            .receber(
                this.http.requisicao().body(Map.of("quantidade", quantidade)).post("/insumos/{id}/entradas-estoque", id)
            );
    }

    private void consultarPagina(final @Nullable UUID cursor) {
        final Map<String, Object> parametros = new HashMap<>();
        parametros.put("first", this.tamanhoPagina);
        if (cursor != null) {
            parametros.put("after", cursor);
        }
        this.http.receber(this.http.requisicao().queryParams(parametros).get("/insumos"));
    }

    private List<RespostaInsumo> insumosEmOrdem() {
        return this.insumosCadastrados.stream().sorted(Comparator.comparing(insumo -> insumo.id().toString())).toList();
    }

    private void validarDadosInsumo(final RespostaInsumo insumo, final DataTable tabela) {
        final var esperado = tabela.asMaps().getFirst();
        assertThat(insumo.id()).isNotNull();
        if (esperado.containsKey("nome")) {
            assertThat(insumo.nome()).isEqualTo(esperado.get("nome"));
        }
        if (esperado.containsKey("precoVenda")) {
            assertThat(insumo.precoVenda()).isEqualByComparingTo(esperado.get("precoVenda"));
        }
    }

    private RespostaLista<RespostaInsumo> listaInsumos() {
        return this.http.resposta().as(new TypeRef<RespostaLista<RespostaInsumo>>() {});
    }

    private RespostaInsumo original() {
        return Objects.requireNonNull(this.insumoOriginal);
    }
}
