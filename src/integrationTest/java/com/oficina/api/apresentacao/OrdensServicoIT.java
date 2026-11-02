package com.oficina.api.apresentacao;

import com.oficina.api.aplicacao.insumo.ServicoInsumos;
import com.oficina.api.aplicacao.ordemservico.ServicoOrdensServico;
import com.oficina.api.apresentacao.cliente.RespostaCliente;
import com.oficina.api.apresentacao.insumo.RespostaInsumo;
import com.oficina.api.apresentacao.ordemservico.RespostaAcessoOrdemServico;
import com.oficina.api.apresentacao.ordemservico.RespostaInsumoNecessario;
import com.oficina.api.apresentacao.ordemservico.RespostaOrdemServico;
import com.oficina.api.apresentacao.servico.RespostaServico;
import com.oficina.api.apresentacao.veiculo.RespostaVeiculo;
import com.oficina.api.dominio.Quantidade;
import com.oficina.api.infraestrutura.email.EnvioEmails;
import com.oficina.api.infraestrutura.seguranca.TokenAcompanhamento;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class OrdensServicoIT extends ApiIntegrationTest {
    @Autowired
    private ServicoOrdensServico servicoOrdens;
    @Autowired
    private ServicoInsumos servicoInsumos;
    @Autowired
    private PlatformTransactionManager transacoes;
    @MockitoBean
    private EnvioEmails emails;
    private @Nullable UUID clienteId;
    private @Nullable UUID veiculoId;
    private @Nullable UUID servicoId;
    private @Nullable UUID insumoId;

    @BeforeEach
    void preparar() {
        limpar();
        final var cliente = requisicao()
            .body(Map.of("nome", "Maria", "email", "maria@example.com", "documento", "52998224725"))
            .post("/clientes");
        validar(cliente, 201);
        this.clienteId = cliente.as(RespostaCliente.class).id();
        final var veiculo = requisicao()
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
            .post("/veiculos");
        validar(veiculo, 201);
        this.veiculoId = veiculo.as(RespostaVeiculo.class).id();
        final var servico = requisicao()
            .body(Map.of("nome", "Revisão", "precoVenda", 100, "requerDiagnostico", false))
            .post("/servicos");
        validar(servico, 201);
        this.servicoId = servico.as(RespostaServico.class).id();
        final var insumo = requisicao().body(Map.of("nome", "Óleo", "precoVenda", 10)).post("/insumos");
        validar(insumo, 201);
        this.insumoId = insumo.as(RespostaInsumo.class).id();
    }

    @AfterEach
    void limpar() {
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

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource(value = "operacoesAdministrativas")
    void deveRequererAutenticacaoNasOperacoesAdministrativas(final String metodo, final String caminho) {
        assertThat(publico().request(metodo, caminho).statusCode()).isEqualTo(401);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource(value = "operacoesAdministrativas")
    void deveRecusarPerfilSemPermissaoAdministrativa(final String metodo, final String caminho) {
        assertThat(
            publico()
                .auth()
                .oauth2(this.emissor.emitir("usuario", Set.of("USER")).valor())
                .request(metodo, caminho)
                .statusCode()
        ).isEqualTo(403);
    }

    private static Stream<Arguments> operacoesAdministrativas() {
        final var base = "/ordens-servico/" + UUID.randomUUID();
        final var insumos = base + "/insumos-necessarios";
        final var item = insumos + "/" + UUID.randomUUID();
        return Stream
            .of(
                Arguments.of("GET", "/ordens-servico"),
                Arguments.of("GET", base),
                Arguments.of("GET", insumos),
                Arguments.of("POST", "/ordens-servico"),
                Arguments.of("POST", base + "/inicio-diagnostico"),
                Arguments.of("POST", base + "/fim-diagnostico"),
                Arguments.of("POST", base + "/inicio-execucao"),
                Arguments.of("POST", base + "/fim-execucao"),
                Arguments.of("POST", base + "/retirada-veiculo"),
                Arguments.of("POST", base + "/acesso-cliente"),
                Arguments.of("POST", insumos),
                Arguments.of("PATCH", item),
                Arguments.of("DELETE", item)
            );
    }

    @Test
    void devePreservarClienteOriginalDaOsAposTrocaDeProprietarioDoVeiculo() {
        final var acesso = criarComDiagnostico();
        assertThat(acesso.ordemServico().clienteId()).isEqualTo(this.clienteId);
        final var cadastro = requisicao()
            .body(Map.of("nome", "João", "email", "joao@example.com", "documento", "11144477735"))
            .post("/clientes");
        validar(cadastro, 201);
        final var novoProprietario = cadastro.as(RespostaCliente.class).id();
        final var transferencia = requisicao()
            .body(Map.of("clienteId", novoProprietario))
            .patch("/veiculos/{id}", this.veiculoId);
        validar(transferencia, 200);
        assertThat(transferencia.as(RespostaVeiculo.class).clienteId()).isEqualTo(novoProprietario);
        final var ordem = buscar(acesso);
        assertThat(ordem.clienteId()).isEqualTo(this.clienteId).isNotEqualTo(novoProprietario);
        assertThat(ordem.veiculoId()).isEqualTo(this.veiculoId);
        diagnosticar(ordem.id());
        assertThat(buscar(acesso).clienteId()).isEqualTo(this.clienteId);
        verify(this.emails, times(2)).enviar(eq(ordem.id()), eq("maria@example.com"), anyString());
        verifyNoMoreInteractions(this.emails);
    }

    @Test
    void deveConcluirDiagnosticoMesmoSeClienteOriginalFoiExcluido() {
        final var acesso = criarComDiagnostico();
        validar(requisicao().delete("/clientes/{id}", this.clienteId), 204);
        final var id = acesso.ordemServico().id();
        diagnosticar(id);
        assertThat(buscar(acesso).status()).isEqualTo("aguardando-confirmacao");
        assertThat(buscar(acesso).clienteId()).isEqualTo(this.clienteId);
        assertThat(corposEnviados(acesso)).hasSize(1);
        validar(publico().get("/ordens-servico/acompanhamento/{token}", acesso.tokenAcompanhamento()), 200);
    }

    @Test
    void deveManterTokensComValidadesIndependentes() {
        final var acesso = criar();
        final var decisao = tokenDecisao(acesso);
        this.jdbc
            .update(
                "UPDATE oficina.ordens_servico SET token_expira_em = now() - interval '1 second' WHERE id = ?",
                acesso.ordemServico().id()
            );
        validar(publico().get("/ordens-servico/acompanhamento/{token}", acesso.tokenAcompanhamento()), 404);
        validar(publico().get("/ordens-servico/decisao/{token}", decisao), 200);
        validar(decidir(acesso, "aprovacao"), 200);
        final var outra = criar();
        this.jdbc
            .update(
                "UPDATE oficina.ordens_servico SET decisao_expira_em = now() - interval '1 second' WHERE id = ?",
                outra.ordemServico().id()
            );
        validar(publico().get("/ordens-servico/acompanhamento/{token}", outra.tokenAcompanhamento()), 200);
        validar(publico().get("/ordens-servico/decisao/{token}", tokenDecisao(outra)), 404);
        validar(decidir(outra, "rejeicao"), 404);
    }

    @Test
    void deveRejeitarClienteInformadoNaCriacaoDaOs() {
        validar(
            requisicao()
                .body(
                    Map
                        .of(
                            "clienteId",
                            Objects.requireNonNull(this.clienteId),
                            "veiculoId",
                            Objects.requireNonNull(this.veiculoId),
                            "servicoIds",
                            List.of(this.servicoId)
                        )
                )
                .post("/ordens-servico"),
            400
        );
        verifyNoInteractions(this.emails);
    }

    @Test
    void deveRejeitarCriacaoQuandoClienteDoVeiculoFoiExcluido() {
        validar(requisicao().delete("/clientes/{id}", this.clienteId), 204);
        validar(criarResposta(), 404);
        verifyNoInteractions(this.emails);
    }

    @Test
    void deveEnviarDoisEmailsESepararPermissoesDosTokens() {
        final var acesso = criar();
        final var corpos = corposEnviados(acesso);
        assertThat(corpos).hasSize(2);
        assertThat(corpos.getFirst())
            .isEqualTo("http://localhost:8080/cliente/" + acesso.tokenAcompanhamento() + "/status");
        final var decisao = tokenDecisao(acesso);
        assertThat(decisao).isNotEqualTo(acesso.tokenAcompanhamento());
        validar(publico().post("/ordens-servico/acompanhamento/{token}/aprovacao", acesso.tokenAcompanhamento()), 404);
        validar(publico().get("/ordens-servico/acompanhamento/{token}", decisao), 404);
        validar(publico().get("/ordens-servico/decisao/{token}", decisao), 200);
        validar(publico().get("/ordens-servico/decisao/{token}", "invalido"), 404);
        for (final var pagina : List.of("status", "aprovacao", "rejeicao")) {
            final var resposta = publico().get("/cliente/{token}/{pagina}", decisao, pagina);
            validar(resposta, 200);
            assertThat(resposta.contentType()).contains("text/html");
            assertThat(resposta.asString()).contains("SwaggerUIBundle", "tryItOutEnabled: true", "validatorUrl: null");
        }
        assertThat(publico().get("/cliente/invalido/status").statusCode()).isEqualTo(404);
        assertThat(buscar(acesso).status()).isEqualTo("aguardando-confirmacao");
        validar(decidir(acesso, "aprovacao"), 200);
        validar(publico().get("/ordens-servico/acompanhamento/{token}", acesso.tokenAcompanhamento()), 200);
    }

    @Test
    void deveRenovarDuranteDiagnosticoEnviandoSomenteAcompanhamento() {
        final var acesso = criarComDiagnostico();
        clearInvocations(this.emails);
        final var resposta = requisicao().post("/ordens-servico/{id}/acesso-cliente", acesso.ordemServico().id());
        validar(resposta, 200);
        final var renovado = resposta.as(RespostaAcessoOrdemServico.class);
        assertThat(corposEnviados(acesso)).hasSize(1);
        assertThat(corposEnviados(acesso).getFirst())
            .isEqualTo("http://localhost:8080/cliente/" + renovado.tokenAcompanhamento() + "/status");
        validar(publico().get("/ordens-servico/acompanhamento/{token}", acesso.tokenAcompanhamento()), 404);
        validar(publico().get("/ordens-servico/acompanhamento/{token}", renovado.tokenAcompanhamento()), 200);
    }

    @Test
    void deveNotificarAposDiagnosticoDeServicosMistosPreservandoAcompanhamento() {
        final var cadastro = requisicao()
            .body(Map.of("nome", "Avaliação do motor", "precoVenda", 50, "requerDiagnostico", true))
            .post("/servicos");
        validar(cadastro, 201);
        final var diagnosticoId = cadastro.as(RespostaServico.class).id();
        final var resposta = requisicao()
            .body(
                Map
                    .of(
                        "veiculoId",
                        Objects.requireNonNull(this.veiculoId),
                        "servicoIds",
                        List.of(this.servicoId, diagnosticoId)
                    )
            )
            .post("/ordens-servico");
        validar(resposta, 201);
        final var acesso = resposta.as(RespostaAcessoOrdemServico.class);
        assertThat(acesso.ordemServico().status()).isEqualTo("aguardando-diagnostico");
        assertThat(corposEnviados(acesso)).hasSize(1);
        final var id = acesso.ordemServico().id();
        diagnosticar(id);
        assertThat(corposEnviados(acesso)).hasSize(2);
        final var token = tokenDecisao(acesso);
        assertThat(
            this.jdbc
                .queryForObject("SELECT hash_token_decisao FROM oficina.ordens_servico WHERE id = ?", String.class, id)
        ).isEqualTo(TokenAcompanhamento.hash(token));
        validar(publico().get("/ordens-servico/acompanhamento/{token}", acesso.tokenAcompanhamento()), 200);
        validar(decidir(acesso, "rejeicao"), 200);
    }

    @Test
    void deveRenovarAcessoInvalidandoTokensAnteriores() {
        final var acesso = criar();
        final var antigo = tokenDecisao(acesso);
        final var resposta = requisicao().post("/ordens-servico/{id}/acesso-cliente", acesso.ordemServico().id());
        validar(resposta, 200);
        final var renovado = resposta.as(RespostaAcessoOrdemServico.class);
        validar(publico().get("/ordens-servico/acompanhamento/{token}", acesso.tokenAcompanhamento()), 404);
        validar(publico().get("/ordens-servico/acompanhamento/{token}", renovado.tokenAcompanhamento()), 200);
        validar(publico().post("/ordens-servico/acompanhamento/{token}/aprovacao", antigo), 404);
        final var novo = tokenDecisao(renovado);
        assertThat(novo).isNotEqualTo(antigo);
        validar(publico().get("/ordens-servico/decisao/{token}", novo), 200);
    }

    @Test
    void deveDescartarNotificacoesPublicadasEmTransacaoRevertida() {
        final var transacao = new TransactionTemplate(this.transacoes);
        transacao.executeWithoutResult(status -> {
            this.servicoOrdens
                .criar(
                    Objects.requireNonNull(this.veiculoId),
                    List.of(Objects.requireNonNull(this.servicoId)),
                    "admin"
                );
            verifyNoInteractions(this.emails);
            status.setRollbackOnly();
        });
        verifyNoInteractions(this.emails);
        assertThat(this.jdbc.queryForObject("SELECT count(*) FROM oficina.ordens_servico", Integer.class)).isZero();
    }

    @Test
    void deveSerializarAprovacaoERejeicaoConcorrentes() throws InterruptedException, ExecutionException {
        final var acesso = criar();
        final var token = tokenDecisao(acesso);
        final var inicio = new CountDownLatch(1);
        try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final var aprovacao = executor.submit(() -> {
                inicio.await();
                return decidir(token, "aprovacao");
            });
            final var rejeicao = executor.submit(() -> {
                inicio.await();
                return decidir(token, "rejeicao");
            });
            inicio.countDown();
            final var respostas = List.of(aprovacao.get(), rejeicao.get());
            assertThat(respostas).extracting(response -> response.statusCode()).containsExactlyInAnyOrder(200, 409);
        }
        final var ordem = buscar(acesso);
        assertThat(ordem.status()).isIn("aguardando-execucao", "aguardando-retirada");
        assertThat(ordem.eventos()).filteredOn(e -> e.tipo().startsWith("ORCAMENTO_")).hasSize(1).allSatisfy(e -> {
            assertThat(e.autor()).isEqualTo("CLIENTE");
            assertThat(e.dados()).containsEntry("total", "100.00");
        });
    }

    @Test
    void devePaginarOrdensMantendoFiltro() {
        final var ordens = List
            .of(criar(), criar(), criar())
            .stream()
            .sorted(Comparator.comparing(a -> a.ordemServico().id().toString()))
            .toList();
        final var primeira = requisicao()
            .queryParams(Map.of("first", 2, "status", "aguardando-confirmacao"))
            .get("/ordens-servico");
        validar(primeira, 200);
        final var pagina = primeira.as(new TypeRef<RespostaLista<RespostaOrdemServico>>() {});
        assertThat(pagina.itens())
            .extracting(resposta -> resposta.id())
            .containsExactly(ordens.getFirst().ordemServico().id(), ordens.get(1).ordemServico().id());
        final var ultima = requisicao()
            .queryParams(Map.of("first", 2, "after", pagina.nextCursor(), "status", "aguardando-confirmacao"))
            .get("/ordens-servico");
        validar(ultima, 200);
        final var finalPagina = ultima.as(new TypeRef<RespostaLista<RespostaOrdemServico>>() {});
        assertThat(finalPagina.itens())
            .extracting(resposta -> resposta.id())
            .containsExactly(ordens.getLast().ordemServico().id());
        assertThat(finalPagina.nextCursor()).isNull();
        final var semFiltro = requisicao().get("/ordens-servico");
        validar(semFiltro, 200);
        assertThat(semFiltro.as(new TypeRef<RespostaLista<RespostaOrdemServico>>() {}).itens()).hasSize(3);
    }

    @Test
    void deveCopiarTodosOsObrigatoriosAlemDeUmaPaginaEPaginarItens() {
        final var insumos = new ArrayList<UUID>();
        for (int indice = 0; indice < 26; indice++) {
            final var insumo = requisicao().body(Map.of("nome", "Peça " + indice, "precoVenda", 1)).post("/insumos");
            validar(insumo, 201);
            final var id = insumo.as(RespostaInsumo.class).id();
            insumos.add(id);
            validar(
                requisicao()
                    .body(Map.of("insumoId", id, "quantidade", 2))
                    .post("/servicos/{id}/insumos-obrigatorios", this.servicoId),
                201
            );
        }
        final var acesso = criar();
        assertThat(acesso.ordemServico().insumosNecessarios()).hasSize(26);
        assertThat(acesso.ordemServico().total()).isEqualByComparingTo("152.00");
        final var primeira = requisicao()
            .queryParam("first", 25)
            .get("/ordens-servico/{id}/insumos-necessarios", acesso.ordemServico().id());
        validar(primeira, 200);
        final var pagina = primeira.as(new TypeRef<RespostaLista<RespostaInsumoNecessario>>() {});
        assertThat(pagina.itens()).hasSize(25);
        final var ultima = requisicao()
            .queryParams(Map.of("first", 25, "after", pagina.nextCursor()))
            .get("/ordens-servico/{id}/insumos-necessarios", acesso.ordemServico().id());
        validar(ultima, 200);
        final var finalPagina = ultima.as(new TypeRef<RespostaLista<RespostaInsumoNecessario>>() {});
        assertThat(finalPagina.itens()).hasSize(1);
        assertThat(finalPagina.nextCursor()).isNull();
        final var copiados = new ArrayList<>(pagina.itens());
        copiados.addAll(finalPagina.itens());
        assertThat(copiados).extracting(resposta -> resposta.insumoId()).containsExactlyInAnyOrderElementsOf(insumos);
    }

    @Test
    void deveIsolarTokensEItensEntreOrdens() {
        validar(requisicao().body(Map.of("requerDiagnostico", true)).patch("/servicos/{id}", this.servicoId), 200);
        final var primeira = criar();
        final var segunda = criar();
        final var id = primeira.ordemServico().id();
        final var outroId = segunda.ordemServico().id();
        validar(requisicao().post("/ordens-servico/{id}/inicio-diagnostico", id), 200);
        validar(requisicao().post("/ordens-servico/{id}/inicio-diagnostico", outroId), 200);
        final var cadastro = requisicao()
            .body(Map.of("insumoId", Objects.requireNonNull(this.insumoId), "quantidade", 3))
            .post("/ordens-servico/{id}/insumos-necessarios", id);
        validar(cadastro, 201);
        final var item = cadastro.as(RespostaInsumoNecessario.class);
        validar(
            requisicao()
                .body(Map.of("quantidade", 4))
                .patch("/ordens-servico/{id}/insumos-necessarios/{item}", outroId, item.id()),
            404
        );
        validar(requisicao().delete("/ordens-servico/{id}/insumos-necessarios/{item}", outroId, item.id()), 404);
        assertThat(buscar(primeira).insumosNecessarios()).containsExactly(item);
        assertThat(buscar(segunda).insumosNecessarios()).isEmpty();
        validar(requisicao().post("/ordens-servico/{id}/fim-diagnostico", id), 200);
        validar(decidir(primeira, "aprovacao"), 200);
        assertThat(buscar(segunda).status()).isEqualTo("em-diagnostico");
        assertThat(primeira.tokenAcompanhamento()).isNotEqualTo(segunda.tokenAcompanhamento());
        assertThat(
            RestAssured
                .given()
                .port(this.porta)
                .auth()
                .oauth2(primeira.tokenAcompanhamento())
                .get("/ordens-servico/{id}", id)
                .statusCode()
        ).isEqualTo(401);
    }

    @ParameterizedTest
    @ValueSource(strings = { "null", "0", "-1", "1.5", "2147483648" })
    void deveValidarQuantidadeNaInclusaoENaAtualizacao(final String quantidade) {
        final var acesso = criarComDiagnostico();
        final var id = acesso.ordemServico().id();
        validar(requisicao().post("/ordens-servico/{id}/inicio-diagnostico", id), 200);
        validar(
            requisicao()
                .body("{\"insumoId\":\"" + this.insumoId + "\",\"quantidade\":" + quantidade + "}")
                .post("/ordens-servico/{id}/insumos-necessarios", id),
            400
        );
        final var cadastro = requisicao()
            .body(Map.of("insumoId", Objects.requireNonNull(this.insumoId), "quantidade", 1))
            .post("/ordens-servico/{id}/insumos-necessarios", id);
        validar(cadastro, 201);
        validar(
            requisicao()
                .body("{\"quantidade\":" + quantidade + "}")
                .patch(
                    "/ordens-servico/{id}/insumos-necessarios/{item}",
                    id,
                    cadastro.as(RespostaInsumoNecessario.class).id()
                ),
            400
        );
        assertThat(buscar(acesso).insumosNecessarios())
            .extracting(resposta -> resposta.quantidade())
            .containsExactly(1);
    }

    @Test
    void deveRejeitarReferenciasInexistentesEExcluidas() {
        final var inexistente = UUID.randomUUID();
        validar(requisicao().get("/ordens-servico/{id}", inexistente), 404);
        validar(requisicao().get("/ordens-servico/{id}/insumos-necessarios", inexistente), 404);
        validar(requisicao().post("/ordens-servico/{id}/inicio-diagnostico", inexistente), 404);
        validar(requisicao().post("/ordens-servico/{id}/acesso-cliente", inexistente), 404);
        validar(
            requisicao()
                .body(Map.of("insumoId", Objects.requireNonNull(this.insumoId), "quantidade", 1))
                .post("/ordens-servico/{id}/insumos-necessarios", inexistente),
            404
        );
        validar(
            requisicao()
                .body(Map.of("quantidade", 1))
                .patch("/ordens-servico/{id}/insumos-necessarios/{item}", inexistente, inexistente),
            404
        );
        final var acesso = criarComDiagnostico();
        final var id = acesso.ordemServico().id();
        validar(requisicao().post("/ordens-servico/{id}/inicio-diagnostico", id), 200);
        validar(
            requisicao()
                .body(Map.of("insumoId", inexistente, "quantidade", 1))
                .post("/ordens-servico/{id}/insumos-necessarios", id),
            404
        );
        validar(requisicao().delete("/insumos/{id}", this.insumoId), 204);
        validar(
            requisicao()
                .body(Map.of("insumoId", Objects.requireNonNull(this.insumoId), "quantidade", 1))
                .post("/ordens-servico/{id}/insumos-necessarios", id),
            404
        );
        assertThat(buscar(acesso).insumosNecessarios()).isEmpty();
        validar(requisicao().delete("/servicos/{id}", this.servicoId), 204);
        validar(criarResposta(), 404);
        validar(requisicao().delete("/veiculos/{id}", this.veiculoId), 204);
        validar(criarResposta(), 404);
        validar(requisicao().delete("/clientes/{id}", this.clienteId), 204);
        validar(criarResposta(), 404);
        assertThat(buscar(acesso).servicos()).hasSize(1);
    }

    @Test
    void deveValidarCorpoEFiltrosDaOrdem() {
        for (final var corpo : List
            .of(
                "{}",
                "{\"servicoIds\":[]}",
                "{\"veiculoId\":null}",
                "{\"veiculoId\":\"invalido\"}",
                "{\"inesperado\":true}"
            )) {
            validar(requisicao().body(corpo).post("/ordens-servico"), 400);
        }
        assertThat(this.jdbc.queryForObject("SELECT count(*) FROM oficina.ordens_servico", Integer.class)).isZero();
        for (final var first : List.of(0, 26)) {
            validar(requisicao().queryParam("first", first).get("/ordens-servico"), 400);
            validar(
                requisicao()
                    .queryParam("first", first)
                    .get("/ordens-servico/{id}/insumos-necessarios", criar().ordemServico().id()),
                400
            );
        }
    }

    @Test
    void deveReservarParcialmenteNaOrdemDeCriacaoMesmoComAprovacoesInvertidasEConsumirNaExecucao() {
        associarInsumo(4);
        final var primeira = criar();
        final var segunda = criar();
        validar(decidir(segunda, "aprovacao"), 200);
        validar(decidir(primeira, "aprovacao"), 200);
        assertThat(buscar(primeira).status()).isEqualTo("aguardando-insumos");
        validar(requisicao().post("/ordens-servico/{id}/inicio-execucao", primeira.ordemServico().id()), 409);
        final var estoque = entrada(Objects.requireNonNull(this.insumoId), 5);
        assertThat(estoque.quantidadeEmEstoque()).isEqualTo(5);
        assertThat(estoque.quantidadeReservada()).isEqualTo(5);
        assertThat(estoque.quantidadeDisponivel()).isZero();
        assertThat(buscar(primeira).status()).isEqualTo("aguardando-execucao");
        assertThat(buscar(segunda).status()).isEqualTo("aguardando-insumos");
        assertThat(buscar(segunda).insumosNecessarios().getFirst().quantidadeReservada()).isEqualTo(1);
        final var eventos = buscar(segunda).eventos();
        validar(decidir(segunda, "aprovacao"), 200);
        assertThat(buscar(segunda).eventos()).isEqualTo(eventos);
        entrada(Objects.requireNonNull(this.insumoId), 3);
        assertThat(buscar(segunda).status()).isEqualTo("aguardando-execucao");
        validar(requisicao().post("/ordens-servico/{id}/inicio-execucao", primeira.ordemServico().id()), 200);
        final var consumida = buscar(primeira).insumosNecessarios().getFirst();
        assertThat(consumida.quantidadeReservada()).isZero();
        assertThat(consumida.quantidadeConsumida()).isEqualTo(4);
        assertThat(consumida.quantidadePendente()).isZero();
        final var saldo = requisicao().get("/insumos/{id}", this.insumoId).as(RespostaInsumo.class);
        assertThat(saldo.quantidadeEmEstoque()).isEqualTo(4);
        assertThat(saldo.quantidadeReservada()).isEqualTo(4);
        validar(requisicao().post("/ordens-servico/{id}/inicio-execucao", primeira.ordemServico().id()), 409);
        validar(decidir(primeira, "aprovacao"), 200);
        assertThat(requisicao().get("/insumos/{id}", this.insumoId).as(RespostaInsumo.class)).isEqualTo(saldo);
    }

    @Test
    void deveReservarNaAprovacaoSemDuplicarEManterSobraDisponivel() {
        associarInsumo(4);
        entrada(Objects.requireNonNull(this.insumoId), 10);
        final var acesso = criar();
        validar(decidir(acesso, "aprovacao"), 200);
        final var ordem = buscar(acesso);
        assertThat(ordem.status()).isEqualTo("aguardando-execucao");
        assertThat(ordem.insumosNecessarios().getFirst().quantidadeReservada()).isEqualTo(4);
        validar(decidir(acesso, "aprovacao"), 200);
        assertThat(buscar(acesso)).isEqualTo(ordem);
        final var estoque = requisicao().get("/insumos/{id}", this.insumoId).as(RespostaInsumo.class);
        assertThat(estoque.quantidadeDisponivel()).isEqualTo(6);
        validar(requisicao().delete("/insumos/{id}", this.insumoId), 409);
    }

    @Test
    void deveManterReservasDeInsumosDiferentesAteCompletarTodos() {
        associarInsumo(2);
        final var outro = requisicao()
            .body(Map.of("nome", "Filtro", "precoVenda", 20))
            .post("/insumos")
            .as(RespostaInsumo.class);
        validar(
            requisicao()
                .body(Map.of("insumoId", outro.id(), "quantidade", 1))
                .post("/servicos/{id}/insumos-obrigatorios", this.servicoId),
            201
        );
        entrada(Objects.requireNonNull(this.insumoId), 2);
        final var acesso = criar();
        validar(decidir(acesso, "aprovacao"), 200);
        assertThat(buscar(acesso).status()).isEqualTo("aguardando-insumos");
        entrada(Objects.requireNonNull(this.insumoId), 3);
        assertThat(buscar(acesso).status()).isEqualTo("aguardando-insumos");
        entrada(outro.id(), 1);
        assertThat(buscar(acesso).status()).isEqualTo("aguardando-execucao");
        assertThat(buscar(acesso).insumosNecessarios()).allSatisfy(i -> assertThat(i.quantidadePendente()).isZero());
    }

    @Test
    void deveSomarMesmoInsumoDeServicosEAvulsoSemReservarEmDuplicidade() {
        associarInsumo(2);
        final var outro = requisicao()
            .body(Map.of("nome", "Complemento", "precoVenda", 20, "requerDiagnostico", false))
            .post("/servicos")
            .as(RespostaServico.class);
        validar(
            requisicao()
                .body(Map.of("insumoId", Objects.requireNonNull(this.insumoId), "quantidade", 3))
                .post("/servicos/{id}/insumos-obrigatorios", outro.id()),
            201
        );
        validar(requisicao().body(Map.of("requerDiagnostico", true)).patch("/servicos/{id}", this.servicoId), 200);
        final var acesso = requisicao()
            .body(
                Map
                    .of(
                        "veiculoId",
                        Objects.requireNonNull(this.veiculoId),
                        "servicoIds",
                        List.of(this.servicoId, outro.id())
                    )
            )
            .post("/ordens-servico")
            .as(RespostaAcessoOrdemServico.class);
        final var id = acesso.ordemServico().id();
        validar(requisicao().post("/ordens-servico/{id}/inicio-diagnostico", id), 200);
        validar(
            requisicao()
                .body(Map.of("insumoId", Objects.requireNonNull(this.insumoId), "quantidade", 4))
                .post("/ordens-servico/{id}/insumos-necessarios", id),
            201
        );
        validar(requisicao().post("/ordens-servico/{id}/fim-diagnostico", id), 200);
        entrada(Objects.requireNonNull(this.insumoId), 6);
        validar(decidir(acesso, "aprovacao"), 200);
        assertThat(buscar(acesso).status()).isEqualTo("aguardando-insumos");
        assertThat(
            buscar(acesso).insumosNecessarios().stream().mapToInt(RespostaInsumoNecessario::quantidadeReservada).sum()
        ).isEqualTo(6);
        entrada(Objects.requireNonNull(this.insumoId), 3);
        assertThat(buscar(acesso).status()).isEqualTo("aguardando-execucao");
        validar(requisicao().post("/ordens-servico/{id}/inicio-execucao", id), 200);
        assertThat(
            buscar(acesso).insumosNecessarios().stream().mapToInt(RespostaInsumoNecessario::quantidadeConsumida).sum()
        ).isEqualTo(9);
        assertThat(requisicao().get("/insumos/{id}", this.insumoId).as(RespostaInsumo.class).quantidadeEmEstoque())
            .isZero();
    }

    @Test
    void deveDesempatarCriacoesPorId() {
        associarInsumo(1);
        final var primeira = criar();
        final var segunda = criar();
        validar(decidir(segunda, "aprovacao"), 200);
        validar(decidir(primeira, "aprovacao"), 200);
        this.jdbc.update("UPDATE oficina.ordens_servico SET criada_em = '2026-01-01T00:00:00Z'");
        entrada(Objects.requireNonNull(this.insumoId), 1);
        assertThat(buscar(primeira).status()).isEqualTo("aguardando-execucao");
        assertThat(buscar(segunda).status()).isEqualTo("aguardando-insumos");
    }

    @Test
    void deveProtegerInsumoAvulsoDeOrdemAtivaMesmoSemServicoAssociado() {
        final var acesso = criarComDiagnostico();
        validar(requisicao().post("/ordens-servico/{id}/inicio-diagnostico", acesso.ordemServico().id()), 200);
        validar(
            requisicao()
                .body(Map.of("insumoId", Objects.requireNonNull(this.insumoId), "quantidade", 1))
                .post("/ordens-servico/{id}/insumos-necessarios", acesso.ordemServico().id()),
            201
        );
        validar(requisicao().delete("/insumos/{id}", this.insumoId), 409);
        validar(requisicao().post("/ordens-servico/{id}/fim-diagnostico", acesso.ordemServico().id()), 200);
        validar(decidir(acesso, "rejeicao"), 200);
        validar(requisicao().delete("/insumos/{id}", this.insumoId), 204);
    }

    @Test
    void deveDisputarUltimasUnidadesSemReservaDupla() throws InterruptedException, ExecutionException {
        associarInsumo(2);
        entrada(Objects.requireNonNull(this.insumoId), 2);
        final var primeira = criar();
        final var segunda = criar();
        final var primeiroToken = tokenDecisao(primeira);
        final var segundoToken = tokenDecisao(segunda);
        final var inicio = new CountDownLatch(1);
        try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final var uma = executor.submit(() -> {
                inicio.await();
                return decidir(primeiroToken, "aprovacao");
            });
            final var outra = executor.submit(() -> {
                inicio.await();
                return decidir(segundoToken, "aprovacao");
            });
            inicio.countDown();
            validar(uma.get(), 200);
            validar(outra.get(), 200);
        }
        assertThat(List.of(buscar(primeira).status(), buscar(segunda).status()))
            .containsExactlyInAnyOrder("aguardando-execucao", "aguardando-insumos");
        assertThat(requisicao().get("/insumos/{id}", this.insumoId).as(RespostaInsumo.class).quantidadeReservada())
            .isEqualTo(2);
    }

    @Test
    void deveSerializarReposicoesConcorrentesSemPerderSaldo() throws InterruptedException, ExecutionException {
        associarInsumo(3);
        final var primeira = criar();
        final var segunda = criar();
        validar(decidir(primeira, "aprovacao"), 200);
        validar(decidir(segunda, "aprovacao"), 200);
        final var insumo = Objects.requireNonNull(this.insumoId);
        final var inicio = new CountDownLatch(1);
        try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final var uma = executor.submit(() -> {
                inicio.await();
                return entrada(insumo, 2);
            });
            final var outra = executor.submit(() -> {
                inicio.await();
                return entrada(insumo, 2);
            });
            inicio.countDown();
            uma.get();
            outra.get();
        }
        assertThat(buscar(primeira).status()).isEqualTo("aguardando-execucao");
        assertThat(buscar(segunda).insumosNecessarios().getFirst().quantidadeReservada()).isEqualTo(1);
        final var saldo = requisicao().get("/insumos/{id}", insumo).as(RespostaInsumo.class);
        assertThat(saldo.quantidadeEmEstoque()).isEqualTo(4);
        assertThat(saldo.quantidadeReservada()).isEqualTo(4);
    }

    @Test
    void deveRenovarAcessoSemConsumirOuLiberarReservasDeOrdemPronta() {
        associarInsumo(2);
        entrada(Objects.requireNonNull(this.insumoId), 3);
        final var acesso = criar();
        validar(decidir(acesso, "aprovacao"), 200);
        final var aprovada = buscar(acesso);
        validar(requisicao().post("/ordens-servico/{id}/acesso-cliente", aprovada.id()), 200);
        final var renovada = buscar(acesso);
        assertThat(renovada.status()).isEqualTo("aguardando-execucao");
        assertThat(renovada.criadaEm()).isEqualTo(aprovada.criadaEm());
        assertThat(renovada.insumosNecessarios()).isEqualTo(aprovada.insumosNecessarios());
        final var estoque = requisicao().get("/insumos/{id}", this.insumoId).as(RespostaInsumo.class);
        assertThat(estoque.quantidadeEmEstoque()).isEqualTo(3);
        assertThat(estoque.quantidadeReservada()).isEqualTo(2);
    }

    @Test
    void deveReverterSaldoReservasEStatusSeTransacaoDaReposicaoFalhar() {
        associarInsumo(2);
        final var acesso = criar();
        validar(decidir(acesso, "aprovacao"), 200);
        final var antes = buscar(acesso);
        final var insumo = Objects.requireNonNull(this.insumoId);
        final var quantidade = new Quantidade(2);
        final var transacao = new TransactionTemplate(this.transacoes);
        assertThatThrownBy(() -> transacao.executeWithoutResult(_ -> {
            this.servicoInsumos.incrementarEstoque(insumo, quantidade);
            throw new IllegalStateException("Falha após alocação");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(buscar(acesso)).isEqualTo(antes);
        final var estoque = requisicao().get("/insumos/{id}", insumo).as(RespostaInsumo.class);
        assertThat(estoque.quantidadeEmEstoque()).isZero();
        assertThat(estoque.quantidadeReservada()).isZero();
    }

    @Test
    void deveCoordenarAprovacaoComReposicaoConcorrente() throws InterruptedException, ExecutionException {
        associarInsumo(2);
        final var primeira = criar();
        final var segunda = criar();
        validar(decidir(primeira, "aprovacao"), 200);
        final var token = tokenDecisao(segunda);
        final var insumo = Objects.requireNonNull(this.insumoId);
        final var inicio = new CountDownLatch(1);
        try (final var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final var aprovacao = executor.submit(() -> {
                inicio.await();
                return decidir(token, "aprovacao");
            });
            final var reposicao = executor.submit(() -> {
                inicio.await();
                return entrada(insumo, 3);
            });
            inicio.countDown();
            validar(aprovacao.get(), 200);
            reposicao.get();
        }
        assertThat(buscar(primeira).status()).isEqualTo("aguardando-execucao");
        assertThat(buscar(segunda).status()).isEqualTo("aguardando-insumos");
        assertThat(buscar(segunda).insumosNecessarios().getFirst().quantidadeReservada()).isEqualTo(1);
        assertThat(requisicao().get("/insumos/{id}", insumo).as(RespostaInsumo.class).quantidadeReservada())
            .isEqualTo(3);
    }

    @Test
    void devePriorizarDataDeCriacaoMesmoQuandoIdsTemOutraOrdem() {
        associarInsumo(1);
        final var primeiroId = criar();
        final var segundoId = criar();
        this.jdbc
            .update(
                "UPDATE oficina.ordens_servico SET criada_em = '2026-01-02T00:00:00Z' WHERE id = ?",
                primeiroId.ordemServico().id()
            );
        this.jdbc
            .update(
                "UPDATE oficina.ordens_servico SET criada_em = '2026-01-01T00:00:00Z' WHERE id = ?",
                segundoId.ordemServico().id()
            );
        validar(decidir(primeiroId, "aprovacao"), 200);
        validar(decidir(segundoId, "aprovacao"), 200);
        entrada(Objects.requireNonNull(this.insumoId), 1);
        assertThat(buscar(segundoId).status()).isEqualTo("aguardando-execucao");
        assertThat(buscar(primeiroId).status()).isEqualTo("aguardando-insumos");
    }

    private void associarInsumo(final int quantidade) {
        validar(
            requisicao()
                .body(Map.of("insumoId", Objects.requireNonNull(this.insumoId), "quantidade", quantidade))
                .post("/servicos/{id}/insumos-obrigatorios", this.servicoId),
            201
        );
    }

    private RespostaInsumo entrada(final UUID id, final int quantidade) {
        final var resposta = requisicao()
            .body(Map.of("quantidade", quantidade))
            .post("/insumos/{id}/entradas-estoque", id);
        validar(resposta, 200);
        return resposta.as(RespostaInsumo.class);
    }

    private RespostaAcessoOrdemServico criarComDiagnostico() {
        validar(requisicao().body(Map.of("requerDiagnostico", true)).patch("/servicos/{id}", this.servicoId), 200);
        return criar();
    }

    private void diagnosticar(final UUID id) {
        validar(requisicao().post("/ordens-servico/{id}/inicio-diagnostico", id), 200);
        validar(requisicao().post("/ordens-servico/{id}/fim-diagnostico", id), 200);
    }

    private List<String> corposEnviados(final RespostaAcessoOrdemServico acesso) {
        final var corpos = ArgumentCaptor.forClass(String.class);
        verify(this.emails, atLeastOnce()).enviar(eq(acesso.ordemServico().id()), anyString(), corpos.capture());
        return corpos.getAllValues();
    }

    private RespostaAcessoOrdemServico criar() {
        final var resposta = criarResposta();
        validar(resposta, 201);
        return resposta.as(RespostaAcessoOrdemServico.class);
    }

    private Response criarResposta() {
        return requisicao()
            .body(Map.of("veiculoId", Objects.requireNonNull(this.veiculoId), "servicoIds", List.of(this.servicoId)))
            .post("/ordens-servico");
    }

    private RespostaOrdemServico buscar(final RespostaAcessoOrdemServico acesso) {
        final var resposta = requisicao().get("/ordens-servico/{id}", acesso.ordemServico().id());
        validar(resposta, 200);
        return resposta.as(RespostaOrdemServico.class);
    }

    private RequestSpecification publico() {
        return RestAssured.given().port(this.porta);
    }

    private String tokenDecisao(final RespostaAcessoOrdemServico acesso) {
        return corposEnviados(acesso)
            .stream()
            .filter(c -> c.contains("/aprovacao"))
            .reduce((_, ultimo) -> ultimo)
            .orElseThrow()
            .split("/", -1)[4];
    }

    private Response decidir(final RespostaAcessoOrdemServico acesso, final String decisao) {
        return decidir(tokenDecisao(acesso), decisao);
    }

    private Response decidir(final String token, final String decisao) {
        return publico().post("/ordens-servico/acompanhamento/{token}/{decisao}", token, decisao);
    }
}
