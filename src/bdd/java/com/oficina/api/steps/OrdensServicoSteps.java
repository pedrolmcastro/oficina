package com.oficina.api.steps;

import com.oficina.api.apresentacao.RespostaLista;
import com.oficina.api.apresentacao.cliente.RespostaCliente;
import com.oficina.api.apresentacao.insumo.RespostaInsumo;
import com.oficina.api.apresentacao.ordemservico.RespostaAcessoOrdemServico;
import com.oficina.api.apresentacao.ordemservico.RespostaInsumoNecessario;
import com.oficina.api.apresentacao.ordemservico.RespostaOrdemServico;
import com.oficina.api.apresentacao.servico.RespostaServico;
import com.oficina.api.apresentacao.veiculo.RespostaVeiculo;
import com.oficina.api.infraestrutura.email.EnvioEmails;
import com.oficina.api.infraestrutura.seguranca.EmissorTokenJwt;
import com.oficina.api.infraestrutura.seguranca.TokenAcompanhamento;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.common.mapper.TypeRef;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

public class OrdensServicoSteps {
    @Autowired
    private EnvioEmails emails;
    @Autowired
    private HttpSteps http;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EmissorTokenJwt emissor;
    private @Nullable UUID clienteId;
    private @Nullable UUID veiculoId;
    private @Nullable UUID insumoId;
    private @Nullable UUID avulsoId;
    private @Nullable UUID automaticoId;
    private @Nullable RespostaAcessoOrdemServico acesso;
    private @Nullable String tokenAnterior;
    private List<UUID> servicoIds = List.of();

    @Before(value = "@ordens-servico")
    public void preparar() {
        limpar();
        administrar();
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("nome", "Maria", "email", "maria@example.com", "documento", "52998224725"))
                    .post("/clientes")
            );
        this.http.validarStatus(201);
        this.clienteId = this.http.resposta().as(RespostaCliente.class).id();
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(
                        Map
                            .of(
                                "placa",
                                "ABC1D23",
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
        this.http.validarStatus(201);
        this.veiculoId = this.http.resposta().as(RespostaVeiculo.class).id();
        this.http.receber(this.http.requisicao().body(Map.of("nome", "Óleo", "precoVenda", 10)).post("/insumos"));
        this.http.validarStatus(201);
        this.insumoId = this.http.resposta().as(RespostaInsumo.class).id();
    }

    @After(value = "@ordens-servico")
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

    @Given("uma OS com um serviço que {string} diagnóstico")
    public void criarOrdem(final String requisitoDiagnostico) {
        administrar();
        final var servico = cadastrarServico("Troca de óleo", "requer".equals(requisitoDiagnostico));
        this.servicoIds = List.of(servico);
        associar(servico, 4);
        criarComAcesso();
    }

    @Given("uma OS com dois serviços, dos quais apenas um requer diagnóstico")
    public void criarOrdemComDoisServicos() {
        administrar();
        final var primeiro = cadastrarServico("Troca de óleo", true);
        final var segundo = cadastrarServico("Limpeza", false);
        this.servicoIds = List.of(primeiro, segundo);
        associar(primeiro, 4);
        associar(segundo, 1);
        criarComAcesso();
    }

    private void criarComAcesso() {
        criar();
        this.http.validarStatus(201);
        this.acesso = this.http.resposta().as(RespostaAcessoOrdemServico.class);
        assertThat(this.http.resposta().header("Location")).isEqualTo("/ordens-servico/" + ordemId());
        final var itens = acesso().ordemServico().insumosNecessarios();
        assertThat(itens).hasSize(this.servicoIds.size());
        assertThat(itens).allSatisfy(i -> assertThat(i.servicoContratadoId()).isNotNull());
        this.automaticoId = itens.getFirst().id();
    }

    @When("executo a ação administrativa {string} na OS")
    public void acao(final String acao) {
        administrar();
        this.http.receber(this.http.requisicao().post("/ordens-servico/{id}/{acao}", ordemId(), acao));
    }

    @Given("uma OS em {string}")
    public void prepararStatus(final String status) {
        criarOrdem("requer");
        if ("aguardando-diagnostico".equals(status)) {
            return;
        }
        acao("inicio-diagnostico");
        this.http.validarStatus(200);
        if ("em-diagnostico".equals(status)) {
            return;
        }
        acao("fim-diagnostico");
        this.http.validarStatus(200);
        if ("aguardando-confirmacao".equals(status)) {
            return;
        }
        decidir("aprovacao");
        this.http.validarStatus(200);
        if ("aguardando-execucao".equals(status)) {
            return;
        }
        acao("inicio-execucao");
        this.http.validarStatus(200);
        if ("em-execucao".equals(status)) {
            return;
        }
        acao("fim-execucao");
        this.http.validarStatus(200);
        assertThat(status).isEqualTo("aguardando-retirada");
    }

    @Given("um insumo avulso incluído com quantidade {int}")
    public void prepararAvulso(final int quantidade) {
        adicionar(quantidade);
        this.http.validarStatus(201);
    }

    @Given("o orçamento já foi {string} pelo cliente")
    public void prepararDecisao(final String decisao) {
        decidir(switch (decisao) {
            case "aprovado" -> "aprovacao";
            case "rejeitado" -> "rejeicao";
            default -> throw new IllegalArgumentException("Decisão desconhecida: " + decisao);
        });
        this.http.validarStatus(200);
    }

    @Given("o diagnóstico foi concluído")
    public void prepararFimDiagnostico() {
        acao("fim-diagnostico");
        this.http.validarStatus(200);
    }

    @Given("o veículo já foi retirado")
    public void prepararRetirada() {
        acao("retirada-veiculo");
        this.http.validarStatus(200);
    }

    @Given("o insumo avulso já foi removido")
    public void prepararRemocaoAvulso() {
        remover("avulso");
        this.http.validarStatus(204);
    }

    @When("consulto a OS administrativamente")
    public void consultar() {
        administrar();
        this.http.receber(this.http.requisicao().get("/ordens-servico/{id}", ordemId()));
    }

    @When("o cliente consulta sua OS pelo link")
    public void acompanhar() {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .auth()
                    .none()
                    .get("/ordens-servico/acompanhamento/{token}", acesso().tokenAcompanhamento())
            );
    }

    @When("o cliente envia {string} pelo link")
    public void decidir(final String decisao) {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .auth()
                    .none()
                    .post("/ordens-servico/acompanhamento/{token}/{decisao}", tokenDecisao(), decisao)
            );
    }

    @Then("a OS está em {string} com total {string}")
    public void validarOrdem(final String status, final String total) {
        assertThat(this.http.resposta().jsonPath().getString("status")).isEqualTo(status);
        assertThat(this.http.resposta().jsonPath().getObject("total", java.math.BigDecimal.class))
            .isEqualByComparingTo(total);
        assertThat(this.http.resposta().header("Cache-Control")).isEqualTo("no-store");
    }

    @Then("o acompanhamento expõe somente os dados necessários ao cliente")
    public void validarPrivacidade() {
        assertThat(this.http.resposta().jsonPath().getMap(""))
            .containsOnlyKeys("status", "resultado", "total", "servicos", "insumosNecessarios");
        final List<Map<String, Object>> servicos = this.http.resposta().jsonPath().getList("servicos");
        assertThat(servicos).isNotEmpty().allSatisfy(servico -> assertThat(servico).containsOnlyKeys("nome", "preco"));
        final List<Map<String, Object>> insumos = this.http.resposta().jsonPath().getList("insumosNecessarios");
        assertThat(insumos)
            .isNotEmpty()
            .allSatisfy(
                insumo -> assertThat(insumo).containsOnlyKeys("nome", "quantidade", "precoUnitario", "subtotal")
            );
        assertThat(this.http.resposta().header("Referrer-Policy")).isEqualTo("no-referrer");
    }

    @Then("o resultado da OS é {string}")
    public void validarResultado(final String resultado) {
        assertThat(this.http.resposta().jsonPath().getString("resultado")).isEqualTo(resultado);
    }

    @When("adiciono um insumo avulso com quantidade {int}")
    public void adicionar(final int quantidade) {
        administrar();
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("insumoId", Objects.requireNonNull(this.insumoId), "quantidade", quantidade))
                    .post("/ordens-servico/{id}/insumos-necessarios", ordemId())
            );
        if (this.http.resposta().statusCode() == 201) {
            this.avulsoId = this.http.resposta().as(RespostaInsumoNecessario.class).id();
        }
    }

    @When("altero o item {string} para quantidade {int}")
    public void alterar(final String origem, final int quantidade) {
        administrar();
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("quantidade", quantidade))
                    .patch("/ordens-servico/{id}/insumos-necessarios/{item}", ordemId(), item(origem))
            );
    }

    @When("removo o item {string}")
    public void remover(final String origem) {
        administrar();
        this.http
            .receber(
                this.http
                    .requisicao()
                    .delete("/ordens-servico/{id}/insumos-necessarios/{item}", ordemId(), item(origem))
            );
    }

    @Then("o item avulso tem quantidade {int}")
    public void validarQuantidade(final int quantidade) {
        assertThat(this.http.resposta().as(RespostaInsumoNecessario.class).quantidade()).isEqualTo(quantidade);
    }

    @When("listo os insumos da OS")
    public void listarInsumos() {
        administrar();
        this.http.receber(this.http.requisicao().get("/ordens-servico/{id}/insumos-necessarios", ordemId()));
    }

    @Then("existe(m) {int} item/itens de insumo(s) com quantidade total {int}")
    public void validarItens(final int itens, final int quantidade) {
        final var lista = this.http.resposta().as(new TypeRef<RespostaLista<RespostaInsumoNecessario>>() {});
        assertThat(lista.itens()).hasSize(itens);
        assertThat(lista.itens().stream().mapToInt(RespostaInsumoNecessario::quantidade).sum()).isEqualTo(quantidade);
    }

    @Then("o insumo obrigatório mantém a quantidade de cada serviço contratado")
    public void validarInsumoPorServico() {
        final var itens = this.http.resposta().as(RespostaOrdemServico.class).insumosNecessarios();
        assertThat(itens).hasSize(2);
        assertThat(itens).extracting(RespostaInsumoNecessario::insumoId).containsOnly(this.insumoId);
        assertThat(itens)
            .extracting(RespostaInsumoNecessario::servicoContratadoId)
            .doesNotHaveDuplicates()
            .doesNotContainNull();
        assertThat(itens).extracting(RespostaInsumoNecessario::quantidade).containsExactlyInAnyOrder(4, 1);
    }

    @Then("o histórico da OS contém em ordem:")
    public void validarHistorico(final DataTable tabela) {
        consultar();
        this.http.validarStatus(200);
        assertThat(this.http.resposta().as(RespostaOrdemServico.class).eventos())
            .extracting(e -> e.tipo())
            .containsExactlyElementsOf(tabela.asList());
        assertThat(this.http.resposta().as(RespostaOrdemServico.class).eventos())
            .extracting(e -> e.sequencia())
            .containsExactlyElementsOf(IntStream.range(0, tabela.height()).boxed().toList());
        assertThat(this.http.resposta().jsonPath().getMap("eventos[0]"))
            .containsOnlyKeys("sequencia", "tipo", "ocorridoEm", "autor", "dados");
    }

    @When("os preços e nomes dos serviços e insumos mudam no catálogo")
    public void alterarCatalogo() {
        administrar();
        for (final var servicoId : this.servicoIds) {
            this.http
                .receber(
                    this.http
                        .requisicao()
                        .body(Map.of("precoVenda", 999, "nome", servicoId.toString()))
                        .patch("/servicos/{id}", servicoId)
                );
            this.http.validarStatus(200);
        }
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("precoVenda", 99, "nome", "Óleo novo"))
                    .patch("/insumos/{id}", this.insumoId)
            );
        this.http.validarStatus(200);
    }

    @Then("os nomes contratados são preservados")
    public void validarNomes() {
        final var ordem = this.http.resposta().as(RespostaOrdemServico.class);
        assertThat(ordem.servicos()).extracting(s -> s.nome()).containsExactly("Troca de óleo");
        assertThat(ordem.insumosNecessarios()).extracting(i -> i.nome()).containsOnly("Óleo");
    }

    @Given("o link de acompanhamento foi reemitido")
    public void renovar() {
        this.tokenAnterior = acesso().tokenAcompanhamento();
        acao("acesso-cliente");
        this.http.validarStatus(200);
        this.acesso = this.http.resposta().as(RespostaAcessoOrdemServico.class);
    }

    @When("o cliente consulta com o token {string}")
    public void consultarToken(final String tipo) {
        final var token = switch (tipo) {
            case "anterior" -> Objects.requireNonNull(this.tokenAnterior);
            case "desconhecido" -> TokenAcompanhamento.gerar();
            default -> "invalido";
        };
        this.http.receber(this.http.requisicao().auth().none().get("/ordens-servico/acompanhamento/{token}", token));
    }

    @Given("o acesso do cliente está expirado")
    public void expirar() {
        this.jdbc
            .update(
                "UPDATE oficina.ordens_servico SET token_expira_em = now() - interval '1 second', "
                    + "decisao_expira_em = now() - interval '1 second' WHERE id = ?",
                ordemId()
            );
    }

    @Then("somente o hash do token está persistido")
    public void validarHash() {
        final var hash = this.jdbc
            .queryForObject("SELECT hash_token FROM oficina.ordens_servico WHERE id = ?", String.class, ordemId());
        assertThat(hash)
            .isEqualTo(TokenAcompanhamento.hash(acesso().tokenAcompanhamento()))
            .isNotEqualTo(acesso().tokenAcompanhamento());
    }

    @When("listo as OS com status {string}")
    public void listar(final String status) {
        administrar();
        this.http.receber(this.http.requisicao().queryParam("status", status).get("/ordens-servico"));
    }

    @Then("a listagem contém {int} OS")
    public void validarLista(final int quantidade) {
        assertThat(this.http.resposta().as(new TypeRef<RespostaLista<RespostaOrdemServico>>() {}).itens())
            .hasSize(quantidade);
    }

    @When("tento criar OS com referência {string}")
    public void criarInvalida(final String tipo) {
        administrar();
        final var veiculo = "veículo inexistente".equals(tipo)
            ? UUID.randomUUID()
            : this.veiculoId;
        final var servicos = switch (tipo) {
            case "serviço inexistente" -> List.of(UUID.randomUUID());
            case "serviço repetido" -> List.of(this.servicoIds.getFirst(), this.servicoIds.getFirst());
            case "sem serviços" -> List.<UUID>of();
            default -> this.servicoIds;
        };
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("veiculoId", Objects.requireNonNull(veiculo), "servicoIds", servicos))
                    .post("/ordens-servico")
            );
    }

    @Then("a criação inválida não persiste outra OS nem eventos")
    public void validarRollback() {
        assertThat(this.jdbc.queryForObject("SELECT count(*) FROM oficina.ordens_servico", Integer.class)).isEqualTo(1);
        assertThat(this.jdbc.queryForObject("SELECT count(*) FROM oficina.ordens_servico_eventos", Integer.class))
            .isEqualTo(acesso().ordemServico().eventos().size());
    }

    @Given("outro insumo disponível para inclusão como avulso")
    public void outroInsumo() {
        administrar();
        this.http.receber(this.http.requisicao().body(Map.of("nome", "Filtro", "precoVenda", 20)).post("/insumos"));
        this.http.validarStatus(201);
        this.insumoId = this.http.resposta().as(RespostaInsumo.class).id();
    }

    private String tokenDecisao() {
        final var corpos = ArgumentCaptor.forClass(String.class);
        verify(this.emails, atLeastOnce()).enviar(eq(ordemId()), anyString(), corpos.capture());
        return corpos
            .getAllValues()
            .stream()
            .filter(c -> c.contains("/aprovacao"))
            .reduce((_, ultimo) -> ultimo)
            .map(c -> c.split("/", -1)[4])
            .orElse(acesso().tokenAcompanhamento());
    }

    private void criar() {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("veiculoId", Objects.requireNonNull(this.veiculoId), "servicoIds", this.servicoIds))
                    .post("/ordens-servico")
            );
    }

    private UUID cadastrarServico(final String nome, final boolean diagnostico) {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("nome", nome, "precoVenda", 100, "requerDiagnostico", diagnostico))
                    .post("/servicos")
            );
        this.http.validarStatus(201);
        return this.http.resposta().as(RespostaServico.class).id();
    }

    private void associar(final UUID servico, final int quantidade) {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("insumoId", Objects.requireNonNull(this.insumoId), "quantidade", quantidade))
                    .post("/servicos/{id}/insumos-obrigatorios", servico)
            );
        this.http.validarStatus(201);
    }

    private void administrar() {
        this.http.autenticar(this.emissor.emitir("admin", Set.of("ADMIN")).valor());
    }
    private RespostaAcessoOrdemServico acesso() {
        return Objects.requireNonNull(this.acesso);
    }
    private UUID ordemId() {
        return acesso().ordemServico().id();
    }
    private UUID item(final String origem) {
        return Objects
            .requireNonNull(
                "avulso".equals(origem)
                    ? this.avulsoId
                    : this.automaticoId
            );
    }
}
