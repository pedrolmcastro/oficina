package com.oficina.api.steps;

import com.oficina.api.apresentacao.RespostaLista;
import com.oficina.api.apresentacao.cliente.RespostaCliente;
import com.oficina.api.apresentacao.insumo.RespostaInsumo;
import com.oficina.api.apresentacao.ordemservico.RespostaAcessoOrdemServico;
import com.oficina.api.apresentacao.ordemservico.RespostaAcompanhamento;
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
import java.util.ArrayList;
import java.util.Comparator;
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
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
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
    private @Nullable RespostaAcessoOrdemServico acesso;
    private @Nullable String tokenAnterior;
    private final List<RespostaAcessoOrdemServico> ordensCadastradas = new ArrayList<>();
    private @Nullable String decisaoAnterior;
    private List<UUID> servicoIds = List.of();

    @Before(value = "@ordens-servico")
    public void preparar() {
        limpar();
        this.http.autenticar(this.emissor.emitir("admin", Set.of("ADMIN")).valor());
    }

    @Given("que existe um veículo e um serviço com diagnóstico {string} para abrir a OS")
    public void prepararCadastro(final String diagnostico) {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("nome", "Maria", "email", "maria@example.com", "documento", "52998224725"))
                    .post("/clientes")
            );
        this.http.validarStatus(201);
        final var clienteId = this.http.resposta().as(RespostaCliente.class).id();
        this.clienteId = clienteId;
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
                                clienteId
                            )
                    )
                    .post("/veiculos")
            );
        this.http.validarStatus(201);
        this.veiculoId = this.http.resposta().as(RespostaVeiculo.class).id();
        this.http.receber(this.http.requisicao().body(Map.of("nome", "Óleo", "precoVenda", 10)).post("/insumos"));
        this.http.validarStatus(201);
        this.insumoId = this.http.resposta().as(RespostaInsumo.class).id();
        final var servico = cadastrarServico("Troca de óleo", "requer".equals(diagnostico));
        this.servicoIds = List.of(servico);
        associar(servico, 4);
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
        prepararCadastro(requisitoDiagnostico);
        criarComAcesso();
    }

    @Given("uma OS sem insumos obrigatórios")
    public void prepararOrdemSemInsumos() {
        prepararCadastro("não requer");
        this.http
            .receber(
                this.http
                    .requisicao()
                    .delete("/servicos/{id}/insumos-obrigatorios/{insumo}", this.servicoIds.getFirst(), this.insumoId)
            );
        this.http.validarStatus(204);
        criarComAcesso();
    }

    @Given("uma OS com dois serviços, dos quais apenas um requer diagnóstico")
    public void criarOrdemComDoisServicos() {
        prepararCadastro("requer");
        final var primeiro = this.servicoIds.getFirst();
        final var segundo = cadastrarServico("Limpeza", false);
        this.servicoIds = List.of(primeiro, segundo);
        associar(segundo, 1);
        criarComAcesso();
    }

    private void criarComAcesso() {
        criar();
        this.http.validarStatus(201);
    }

    @When("executo a ação administrativa {string} na OS")
    public void acao(final String acao) {
        this.http.receber(this.http.requisicao().post("/ordens-servico/{id}/{acao}", ordemId(), acao));
    }

    @Given("uma OS em {string}")
    public void prepararStatus(final String status) {
        assertThat(status)
            .isIn(
                "aguardando-diagnostico",
                "em-diagnostico",
                "aguardando-confirmacao",
                "aguardando-insumos",
                "aguardando-execucao",
                "em-execucao",
                "aguardando-retirada"
            );
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
        if (!"aguardando-insumos".equals(status)) {
            incrementarEstoque(4);
            this.http.validarStatus(200);
        }
        decidir("aprovacao");
        this.http.validarStatus(200);
        if ("aguardando-insumos".equals(status)) {
            return;
        }
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

    @Given("uma entrada de {int} unidades do insumo")
    public void prepararEstoque(final int quantidade) {
        incrementarEstoque(quantidade);
        this.http.validarStatus(200);
    }

    @When("incremento o estoque do insumo em {int} unidades")
    public void incrementarEstoque(final int quantidade) {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("quantidade", quantidade))
                    .post("/insumos/{id}/entradas-estoque", this.insumoId)
            );
    }

    @Then("a OS possui {int} unidades reservadas e {int} pendentes")
    public void validarReservas(final int reservada, final int pendente) {
        consultar();
        this.http.validarStatus(200);
        final var itens = this.http.resposta().as(RespostaOrdemServico.class).insumosNecessarios();
        assertThat(itens.stream().mapToInt(RespostaInsumoNecessario::quantidadeReservada).sum()).isEqualTo(reservada);
        assertThat(itens.stream().mapToInt(RespostaInsumoNecessario::quantidadePendente).sum()).isEqualTo(pendente);
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

    @When("consulto a OS administrativamente")
    public void consultar() {
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

    @Then("a resposta deve conter a OS:")
    public void validarOrdem(final DataTable tabela) {
        validarCamposOrdem(tabela.asMaps().getFirst());
    }

    @Then("a OS deve estar em {string}")
    public void validarStatusOrdem(final String status) {
        validarCamposOrdem(Map.of("status", status));
    }

    @Then("a OS deve estar em {string} com total {string}")
    public void validarStatusETotalOrdem(final String status, final String total) {
        validarCamposOrdem(Map.of("status", status, "total", total));
    }

    private void validarCamposOrdem(final Map<@Nullable String, @Nullable String> esperado) {
        final var json = this.http.resposta().jsonPath();
        final var prefixo = json.getMap("").containsKey("ordemServico")
            ? "ordemServico."
            : "";
        for (final var campo : List.of("status", "resultado")) {
            if (esperado.containsKey(campo)) {
                assertThat(json.getString(prefixo + campo)).isEqualTo(esperado.get(campo));
            }
        }
        if (esperado.containsKey("total")) {
            assertThat(json.getObject(prefixo + "total", java.math.BigDecimal.class))
                .isEqualByComparingTo(esperado.get("total"));
        }
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

    @When("adiciono um insumo avulso com quantidade {int}")
    public void adicionar(final int quantidade) {
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
        removerPorId(item(origem).toString());
    }

    @When("removo o insumo necessário pelo ID {string}")
    public void removerPorId(final String id) {
        this.http
            .receber(this.http.requisicao().delete("/ordens-servico/{id}/insumos-necessarios/{item}", ordemId(), id));
    }

    @Then("o item avulso tem quantidade {int}")
    public void validarQuantidade(final int quantidade) {
        assertThat(this.http.resposta().as(RespostaInsumoNecessario.class).quantidade()).isEqualTo(quantidade);
    }

    @When("consulto os insumos necessários da OS")
    public void consultarInsumosNecessarios() {
        this.http.receber(this.http.requisicao().get("/ordens-servico/{id}/insumos-necessarios", ordemId()));
    }

    @Then("existe(m) {int} item/itens de insumo(s) necessário(s) com quantidade total {int}")
    public void validarInsumosNecessarios(final int itens, final int quantidade) {
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
    }

    @When("os preços e nomes dos serviços e insumos mudam nos cadastros")
    public void alterarCadastrosServicosInsumos() {
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
        this.decisaoAnterior = tokenDecisao();
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

    @When("consulto as OS com status {string}")
    public void consultarPorStatus(final String status) {
        this.http.receber(this.http.requisicao().queryParam("status", status).get("/ordens-servico"));
    }

    @Then("a lista de OS contém {int} OS")
    public void validarLista(final int quantidade) {
        assertThat(this.http.resposta().as(new TypeRef<RespostaLista<RespostaOrdemServico>>() {}).itens())
            .hasSize(quantidade);
    }

    @When("tento criar OS com referência {string}")
    public void criarInvalida(final String tipo) {
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

    @Then("a lista de OS deve estar vazia")
    public void validarListaVazia() {
        assertThat(listaOrdens().itens()).isEmpty();
        assertThat(listaOrdens().nextCursor()).isNull();
    }

    @Given("outro insumo disponível para inclusão como avulso")
    public void outroInsumo() {
        this.http.receber(this.http.requisicao().body(Map.of("nome", "Filtro", "precoVenda", 20)).post("/insumos"));
        this.http.validarStatus(201);
        this.insumoId = this.http.resposta().as(RespostaInsumo.class).id();
    }

    @Then("a OS criada deve pertencer ao veículo e ao cliente cadastrados")
    public void validarCriacao() {
        final var ordem = acesso().ordemServico();
        assertThat(ordem.veiculoId()).isEqualTo(this.veiculoId);
        final var veiculo = this.http.requisicao().get("/veiculos/{id}", this.veiculoId);
        assertThat(veiculo.statusCode()).isEqualTo(200);
        assertThat(ordem.clienteId()).isEqualTo(veiculo.as(RespostaVeiculo.class).clienteId());
        assertThat(this.http.resposta().header("Location")).isEqualTo("/ordens-servico/" + ordem.id());
        assertThat(ordem.servicos())
            .extracting(servico -> servico.servicoId())
            .containsExactlyElementsOf(this.servicoIds);
    }

    @When("consulto as OS")
    public void consultarTodas() {
        this.http.receber(this.http.requisicao().get("/ordens-servico"));
    }

    @When("consulto a OS pelo ID {string}")
    public void consultarPorId(final String id) {
        this.http.receber(this.http.requisicao().get("/ordens-servico/{id}", id));
    }

    @Then("o estoque utilizado pela OS deve ter {int} unidades físicas, {int} reservadas e {int} disponíveis")
    public void validarEstoque(final int fisico, final int reservado, final int disponivel) {
        final var resposta = this.http.requisicao().get("/insumos/{id}", this.insumoId);
        assertThat(resposta.statusCode()).isEqualTo(200);
        final var insumo = resposta.as(RespostaInsumo.class);
        assertThat(insumo.quantidadeEmEstoque()).isEqualTo(fisico);
        assertThat(insumo.quantidadeReservada()).isEqualTo(reservado);
        assertThat(insumo.quantidadeDisponivel()).isEqualTo(disponivel);
    }

    @Then("os insumos necessários da OS devem ter as quantidades:")
    public void validarQuantidadesInsumosNecessarios(final DataTable tabela) {
        final var resposta = this.http.requisicao().get("/ordens-servico/{id}", ordemId());
        assertThat(resposta.statusCode()).isEqualTo(200);
        final var itens = resposta.as(RespostaOrdemServico.class).insumosNecessarios();
        assertThat(itens).hasSize(tabela.asMaps().size());
        for (final var esperado : tabela.asMaps()) {
            assertThat(itens)
                .filteredOn(item -> item.nome().equals(esperado.get("nome")))
                .filteredOn(
                    item -> (item.servicoContratadoId() == null
                        ? "avulso"
                        : "obrigatório").equals(esperado.get("origem"))
                )
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.quantidade()).isEqualTo(Integer.parseInt(esperado.get("quantidade")));
                    assertThat(item.quantidadeReservada()).isEqualTo(Integer.parseInt(esperado.get("reservada")));
                    assertThat(item.quantidadeConsumida()).isEqualTo(Integer.parseInt(esperado.get("consumida")));
                    assertThat(item.quantidadePendente()).isEqualTo(Integer.parseInt(esperado.get("pendente")));
                });
        }
    }

    @Given("outra OS aprovada para o mesmo insumo")
    public void prepararOutraOrdem() {
        criarComAcesso();
        acao("inicio-diagnostico");
        this.http.validarStatus(200);
        prepararFimDiagnostico();
        prepararDecisao("aprovado");
    }

    @When("consulto a OS cadastrada {int}")
    public void selecionarOrdem(final int posicao) {
        this.acesso = this.ordensCadastradas.get(posicao - 1);
        consultar();
    }

    @When("o cliente consulta o orçamento pelo link de decisão")
    public void consultarDecisao() {
        this.http.receber(this.http.requisicao().auth().none().get("/ordens-servico/decisao/{token}", tokenDecisao()));
    }

    @When("o cliente envia {string} com o link de acompanhamento")
    public void decidirComAcompanhamento(final String decisao) {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .auth()
                    .none()
                    .post("/ordens-servico/acompanhamento/{token}/{decisao}", acesso().tokenAcompanhamento(), decisao)
            );
    }

    @When("o cliente envia {string} com o link de decisão anterior")
    public void decidirComLinkAnterior(final String decisao) {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .auth()
                    .none()
                    .post("/ordens-servico/acompanhamento/{token}/{decisao}", this.decisaoAnterior, decisao)
            );
    }

    @Then("o orçamento do cliente deve conter os itens contratados")
    public void validarOrcamentoCliente() {
        final var resposta = this.http.resposta().as(RespostaAcompanhamento.class);
        assertThat(resposta.servicos()).singleElement().satisfies(servico -> {
            assertThat(servico.nome()).isEqualTo("Troca de óleo");
            assertThat(servico.preco()).isEqualByComparingTo("100.00");
        });
        assertThat(resposta.insumosNecessarios()).singleElement().satisfies(insumo -> {
            assertThat(insumo.nome()).isEqualTo("Óleo");
            assertThat(insumo.quantidade()).isEqualTo(4);
            assertThat(insumo.precoUnitario()).isEqualByComparingTo("10.00");
            assertThat(insumo.subtotal()).isEqualByComparingTo("40.00");
        });
    }

    @Given("que existem {int} OS aguardando diagnóstico")
    public void prepararOrdens(final int quantidade) {
        prepararCadastro("requer");
        for (int indice = 0; indice < quantidade; indice++) {
            criarComAcesso();
        }
    }

    @When("consulto as OS aguardando diagnóstico com limite 1")
    public void consultarPrimeiraPagina() {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .queryParam("status", "aguardando-diagnostico")
                    .queryParam("first", 1)
                    .get("/ordens-servico")
            );
    }

    @When("consulto a próxima página de OS aguardando diagnóstico")
    public void consultarProximaPagina() {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .queryParam("status", "aguardando-diagnostico")
                    .queryParam("first", 1)
                    .queryParam("after", Objects.requireNonNull(listaOrdens().nextCursor()))
                    .get("/ordens-servico")
            );
    }

    @Then("a página deve conter a OS {int} em ordem de ID")
    public void validarPagina(final int posicao) {
        final var ordens = this.ordensCadastradas
            .stream()
            .map(RespostaAcessoOrdemServico::ordemServico)
            .sorted(Comparator.comparing(ordem -> ordem.id().toString()))
            .toList();
        assertThat(listaOrdens().itens())
            .extracting(RespostaOrdemServico::id)
            .containsExactly(ordens.get(posicao - 1).id());
    }

    @Then("a resposta deve indicar a próxima página de OS")
    public void validarProximaPagina() {
        assertThat(listaOrdens().nextCursor()).isEqualTo(listaOrdens().itens().getLast().id());
    }

    @Then("a resposta deve indicar o fim da lista de OS")
    public void validarFimLista() {
        assertThat(listaOrdens().nextCursor()).isNull();
    }

    private RespostaLista<RespostaOrdemServico> listaOrdens() {
        return this.http.resposta().as(new TypeRef<RespostaLista<RespostaOrdemServico>>() {});
    }

    @Then("o cliente original recebe {int} email(s) de acompanhamento e {int} email(s) de decisão")
    public void validarNotificacoes(final int acompanhamento, final int decisao) {
        final var destinatarios = ArgumentCaptor.forClass(String.class);
        final var corpos = ArgumentCaptor.forClass(String.class);
        verify(this.emails, times(acompanhamento + decisao))
            .enviar(eq(ordemId()), destinatarios.capture(), corpos.capture());
        assertThat(destinatarios.getAllValues()).containsOnly("maria@example.com");
        assertThat(corpos.getAllValues().stream().filter(c -> c.endsWith("/status"))).hasSize(acompanhamento);
        assertThat(corpos.getAllValues().stream().filter(c -> c.contains("/aprovacao") && c.endsWith("/rejeicao")))
            .hasSize(decisao);
        if (acompanhamento > 0) {
            assertThat(corpos.getAllValues())
                .anySatisfy(c -> assertThat(c).endsWith("/cliente/" + acesso().tokenAcompanhamento() + "/status"));
        }
    }

    @Given("as notificações anteriores foram recebidas")
    public void limparNotificacoes() {
        clearInvocations(this.emails);
    }

    @When("reemito os links do cliente")
    public void reemitirLinks() {
        acao("acesso-cliente");
        this.http.validarStatus(200);
        this.acesso = this.http.resposta().as(RespostaAcessoOrdemServico.class);
    }

    @When("transfiro o veículo da OS para outro cliente")
    public void transferirVeiculo() {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("nome", "João", "email", "joao@example.com", "documento", "11144477735"))
                    .post("/clientes")
            );
        this.http.validarStatus(201);
        final var novoCliente = this.http.resposta().as(RespostaCliente.class).id();
        this.http
            .receber(
                this.http.requisicao().body(Map.of("clienteId", novoCliente)).patch("/veiculos/{id}", this.veiculoId)
            );
    }

    @Then("a OS continua vinculada ao cliente original e ao mesmo veículo")
    public void validarClienteOriginal() {
        final var ordem = this.http.resposta().as(RespostaOrdemServico.class);
        assertThat(ordem.clienteId()).isEqualTo(this.clienteId);
        assertThat(ordem.veiculoId()).isEqualTo(this.veiculoId);
    }

    @Then("o item avulso tem preço unitário {string} e subtotal {string}")
    public void validarValoresAvulso(final String preco, final String subtotal) {
        final var item = this.http.resposta().as(RespostaInsumoNecessario.class);
        assertThat(item.precoUnitario()).isEqualByComparingTo(preco);
        assertThat(item.subtotal()).isEqualByComparingTo(subtotal);
    }

    @Then("o orçamento do cliente contém o avulso com quantidade {int} e subtotal {string}")
    public void validarAvulsoNoOrcamento(final int quantidade, final String subtotal) {
        final var itens = this.http.resposta().as(RespostaAcompanhamento.class).insumosNecessarios();
        assertThat(itens).hasSize(2);
        assertThat(itens).anySatisfy(item -> {
            assertThat(item.quantidade()).isEqualTo(quantidade);
            assertThat(item.precoUnitario()).isEqualByComparingTo("10.00");
            assertThat(item.subtotal()).isEqualByComparingTo(subtotal);
        });
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
            .orElseThrow();
    }

    @When("abro a OS para o veículo com o serviço cadastrado")
    public void criar() {
        this.http
            .receber(
                this.http
                    .requisicao()
                    .body(Map.of("veiculoId", Objects.requireNonNull(this.veiculoId), "servicoIds", this.servicoIds))
                    .post("/ordens-servico")
            );
        if (this.http.resposta().statusCode() == 201) {
            this.acesso = this.http.resposta().as(RespostaAcessoOrdemServico.class);
            this.ordensCadastradas.add(acesso());
        }
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
                    : acesso().ordemServico().insumosNecessarios().getFirst().id()
            );
    }
}
