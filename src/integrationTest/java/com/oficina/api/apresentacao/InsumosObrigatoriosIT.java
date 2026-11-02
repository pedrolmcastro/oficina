package com.oficina.api.apresentacao;

import com.oficina.api.apresentacao.insumo.RespostaInsumo;
import com.oficina.api.apresentacao.servico.RespostaInsumoObrigatorio;
import com.oficina.api.apresentacao.servico.RespostaServico;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.ProblemDetail;
import static org.assertj.core.api.Assertions.assertThat;

class InsumosObrigatoriosIT extends ApiIntegrationTest {
    @BeforeEach
    void preparar() {
        this.jdbc.update("DELETE FROM oficina.servicos_insumos_obrigatorios");
        this.jdbc.update("DELETE FROM oficina.servicos");
        this.jdbc.update("DELETE FROM oficina.insumos");
    }

    @Test
    void deveRetornarLocalizacaoEContratoDaAssociacao() {
        final var servico = criarServico("Troca de óleo");
        final var insumo = criarInsumo("Óleo");
        final var cadastro = associar(servico.id(), insumo.id());
        validar(cadastro, 201);
        assertThat(cadastro.header("Location"))
            .isEqualTo("/servicos/" + servico.id() + "/insumos-obrigatorios/" + insumo.id());
        assertThat(cadastro.as(RespostaInsumoObrigatorio.class)).isEqualTo(obrigatorio(insumo));
        assertThat(cadastro.jsonPath().getMap("")).containsOnlyKeys("insumoId", "quantidade");
    }

    @Test
    void deveAtualizarSomenteAAssociacaoInformada() {
        final var servico = criarServico("Troca");
        final var outro = criarServico("Revisão");
        final var insumo = criarInsumo("Óleo");
        final var filtro = criarInsumo("Filtro");
        validar(associar(servico.id(), insumo.id()), 201);
        validar(associar(servico.id(), filtro.id()), 201);
        validar(associar(outro.id(), insumo.id()), 201);
        for (final var quantidade : List.of(4, 4, 1)) {
            final var resposta = requisicao()
                .contentType("application/merge-patch+json")
                .body(Map.of("quantidade", quantidade))
                .patch("/servicos/{id}/insumos-obrigatorios/{insumo}", servico.id(), insumo.id());
            validar(resposta, 200);
            final var esperado = new RespostaInsumoObrigatorio(insumo.id(), quantidade);
            assertThat(resposta.as(RespostaInsumoObrigatorio.class)).isEqualTo(esperado);
            assertThat(listar(servico.id()).itens()).containsExactlyInAnyOrder(esperado, obrigatorio(filtro));
            assertThat(listar(outro.id()).itens()).containsExactly(obrigatorio(insumo));
        }
        assertThat(requisicao().get("/insumos/{id}", insumo.id()).as(RespostaInsumo.class)).isEqualTo(insumo);
    }

    @ParameterizedTest
    @ValueSource(strings = { "{}", "{\"quantidade\":null}", "{\"quantidade\":1.5}", "{\"quantidade\":2147483648}" })
    void deveRejeitarAtualizacaoComDadosInvalidosSemAlterarAssociacao(final String corpo) {
        final var servico = criarServico("Troca");
        final var insumo = criarInsumo("Óleo");
        validar(associar(servico.id(), insumo.id()), 201);
        validar(
            requisicao().body(corpo).patch("/servicos/{id}/insumos-obrigatorios/{insumo}", servico.id(), insumo.id()),
            400
        );
        assertThat(listar(servico.id()).itens()).containsExactly(obrigatorio(insumo));
    }

    @Test
    void deveRejeitarAtualizacaoDeAssociacaoRemovida() {
        final var servico = criarServico("Troca");
        final var insumo = criarInsumo("Óleo");
        validar(associar(servico.id(), insumo.id()), 201);
        validar(remover(servico.id(), insumo.id()), 204);
        validar(
            requisicao()
                .body(Map.of("quantidade", 4))
                .patch("/servicos/{id}/insumos-obrigatorios/{insumo}", servico.id(), insumo.id()),
            404
        );
        assertThat(listar(servico.id()).itens()).isEmpty();
    }

    @Test
    void devePreservarVinculosAoAtualizarERemoverSemExcluirInsumo() {
        final var servico = criarServico("Troca de óleo");
        final var outro = criarServico("Revisão");
        final var insumo = criarInsumo("Óleo");
        validar(associar(servico.id(), insumo.id()), 201);
        validar(associar(outro.id(), insumo.id()), 201);
        validar(
            requisicao()
                .body(Map.of("nome", "Troca completa", "requerDiagnostico", false))
                .patch("/servicos/{id}", servico.id()),
            200
        );
        assertThat(listar(servico.id()).itens()).containsExactly(obrigatorio(insumo));
        validar(remover(servico.id(), insumo.id()), 204);
        validar(remover(servico.id(), insumo.id()), 404);
        assertThat(listar(servico.id()).itens()).isEmpty();
        assertThat(listar(outro.id()).itens()).containsExactly(obrigatorio(insumo));
        final var consulta = requisicao().get("/insumos/{id}", insumo.id());
        validar(consulta, 200);
        assertThat(consulta.as(RespostaInsumo.class)).isEqualTo(insumo);
        validar(associar(servico.id(), insumo.id()), 201);
    }

    @Test
    void devePaginarApenasInsumosDoServicoEmOrdem() {
        final var servico = criarServico("Revisão");
        final var insumos = List
            .of(criarInsumo("Óleo"), criarInsumo("Filtro"), criarInsumo("Pneu"))
            .stream()
            .sorted(java.util.Comparator.comparing(i -> i.id().toString()))
            .toList();
        criarInsumo("Sem associação");
        for (final var insumo : insumos.reversed()) {
            validar(associar(servico.id(), insumo.id()), 201);
        }
        final var primeiraResposta = requisicao()
            .queryParam("first", 2)
            .get("/servicos/{id}/insumos-obrigatorios", servico.id());
        validar(primeiraResposta, 200);
        assertThat(primeiraResposta.jsonPath().getMap("itens[0]")).containsOnlyKeys("insumoId", "quantidade");
        final var primeira = primeiraResposta.as(new TypeRef<RespostaLista<RespostaInsumoObrigatorio>>() {});
        assertThat(primeira.itens())
            .containsExactlyElementsOf(insumos.subList(0, 2).stream().map(InsumosObrigatoriosIT::obrigatorio).toList());
        assertThat(primeira.nextCursor()).isEqualTo(insumos.get(1).id());
        final var ultimaResposta = requisicao()
            .queryParams(Map.of("first", 2, "after", primeira.nextCursor()))
            .get("/servicos/{id}/insumos-obrigatorios", servico.id());
        validar(ultimaResposta, 200);
        final var ultima = ultimaResposta.as(new TypeRef<RespostaLista<RespostaInsumoObrigatorio>>() {});
        assertThat(ultima.itens()).containsExactly(obrigatorio(insumos.getLast()));
        assertThat(ultima.nextCursor()).isNull();
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void deveRejeitarInsumoInexistenteOuExcluido(final boolean excluir) {
        final var servico = criarServico("Revisão");
        final var insumoId = excluir
            ? criarInsumo("Óleo").id()
            : UUID.randomUUID();
        if (excluir) {
            validar(requisicao().delete("/insumos/{id}", insumoId), 204);
        }
        validar(associar(servico.id(), insumoId), 404);
        validar(remover(servico.id(), insumoId), 404);
        assertThat(listar(servico.id()).itens()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void deveRejeitarOperacoesEmServicoInexistenteOuExcluido(final boolean excluir) {
        final var insumo = criarInsumo("Óleo");
        final var servicoId = excluir
            ? criarServico("Revisão").id()
            : UUID.randomUUID();
        if (excluir) {
            validar(associar(servicoId, insumo.id()), 201);
            validar(requisicao().delete("/servicos/{id}", servicoId), 204);
        }
        validar(associar(servicoId, insumo.id()), 404);
        validar(
            requisicao()
                .body(Map.of("quantidade", 4))
                .patch("/servicos/{id}/insumos-obrigatorios/{insumo}", servicoId, insumo.id()),
            404
        );
        validar(remover(servicoId, insumo.id()), 404);
        final var resposta = requisicao().get("/servicos/{id}/insumos-obrigatorios", servicoId);
        validar(resposta, 404);
        assertThat(resposta.as(ProblemDetail.class).getDetail()).isEqualTo("Serviço não encontrado");
    }

    @Test
    void deveImpedirExcluirInsumoObrigatorioAteRemoverTodosOsVinculosAtivos() {
        final var primeiro = criarServico("Troca");
        final var segundo = criarServico("Revisão");
        final var insumo = criarInsumo("Óleo");
        validar(associar(primeiro.id(), insumo.id()), 201);
        validar(associar(segundo.id(), insumo.id()), 201);
        validar(requisicao().delete("/insumos/{id}", insumo.id()), 409);
        assertThat(listar(primeiro.id()).itens()).containsExactly(obrigatorio(insumo));
        validar(remover(primeiro.id(), insumo.id()), 204);
        validar(requisicao().delete("/insumos/{id}", insumo.id()), 409);
        validar(requisicao().delete("/servicos/{id}", segundo.id()), 204);
        validar(requisicao().delete("/insumos/{id}", insumo.id()), 204);
    }

    @Test
    void devePreservarAssociacaoAoAtualizarInsumo() {
        final var servico = criarServico("Troca");
        final var insumo = criarInsumo("Óleo");
        validar(associar(servico.id(), insumo.id()), 201);
        validar(
            requisicao().body(Map.of("nome", "Óleo sintético", "precoVenda", 25)).patch("/insumos/{id}", insumo.id()),
            200
        );
        final var associado = listar(servico.id()).itens().getFirst();
        assertThat(associado).isEqualTo(obrigatorio(insumo));
    }

    @ParameterizedTest
    @ValueSource(strings = { "1.5", "0.9", "2147483648" })
    void deveRejeitarQuantidadeInvalidaSemCriarVinculo(final String quantidade) {
        final var servico = criarServico("Troca");
        final var insumo = criarInsumo("Óleo");
        validar(
            requisicao()
                .body(Map.of("insumoId", insumo.id(), "quantidade", new BigDecimal(quantidade)))
                .post("/servicos/{id}/insumos-obrigatorios", servico.id()),
            400
        );
        assertThat(listar(servico.id()).itens()).isEmpty();
    }

    private static RespostaInsumoObrigatorio obrigatorio(final RespostaInsumo insumo) {
        return new RespostaInsumoObrigatorio(insumo.id(), 2);
    }

    private RespostaServico criarServico(final String nome) {
        final var resposta = requisicao()
            .body(Map.of("nome", nome, "precoVenda", 100, "requerDiagnostico", false))
            .post("/servicos");
        validar(resposta, 201);
        return resposta.as(RespostaServico.class);
    }

    private RespostaInsumo criarInsumo(final String nome) {
        final var resposta = requisicao().body(Map.of("nome", nome, "precoVenda", 10)).post("/insumos");
        validar(resposta, 201);
        return resposta.as(RespostaInsumo.class);
    }

    private Response associar(final UUID servico, final UUID insumo) {
        return requisicao()
            .body(Map.of("insumoId", insumo, "quantidade", 2))
            .post("/servicos/{id}/insumos-obrigatorios", servico);
    }

    private Response remover(final UUID servico, final UUID insumo) {
        return requisicao().delete("/servicos/{id}/insumos-obrigatorios/{insumo}", servico, insumo);
    }

    private RespostaLista<RespostaInsumoObrigatorio> listar(final UUID servico) {
        final var resposta = requisicao().get("/servicos/{id}/insumos-obrigatorios", servico);
        validar(resposta, 200);
        return resposta.as(new TypeRef<RespostaLista<RespostaInsumoObrigatorio>>() {});
    }
}
