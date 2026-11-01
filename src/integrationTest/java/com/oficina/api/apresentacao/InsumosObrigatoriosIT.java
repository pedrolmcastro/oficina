package com.oficina.api.apresentacao;

import com.oficina.api.apresentacao.insumo.RespostaInsumo;
import com.oficina.api.apresentacao.servico.RespostaInsumoObrigatorio;
import com.oficina.api.apresentacao.servico.RespostaServico;
import io.restassured.RestAssured;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    void deveAssociarSemDuplicar() {
        final var servico = criarServico("Troca de óleo");
        final var outro = criarServico("Revisão");
        final var insumo = criarInsumo("Óleo");
        assertThat(listar(servico.id()).itens()).isEmpty();
        final var cadastro = associar(servico.id(), insumo.id());
        validar(cadastro, 201);
        assertThat(cadastro.header("Location"))
            .isEqualTo("/servicos/" + servico.id() + "/insumos-obrigatorios/" + insumo.id());
        assertThat(cadastro.as(RespostaInsumoObrigatorio.class)).isEqualTo(obrigatorio(insumo));
        assertThat(cadastro.jsonPath().getMap("")).containsOnlyKeys("insumoId", "quantidade");
        validar(associar(servico.id(), insumo.id()), 409);
        assertThat(listar(servico.id()).itens()).containsExactly(obrigatorio(insumo));
        assertThat(listar(outro.id()).itens()).isEmpty();
        validar(associar(outro.id(), insumo.id()), 201);
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
    void deveExigirAdministradorParaGerenciarServicosEVinculos() {
        for (final var caminho : List
            .of(
                "/servicos",
                "/servicos/" + UUID.randomUUID(),
                "/servicos/" + UUID.randomUUID() + "/insumos-obrigatorios"
            )) {
            assertThat(RestAssured.given().port(this.porta).get(caminho).statusCode()).isEqualTo(401);
            assertThat(
                RestAssured
                    .given()
                    .port(this.porta)
                    .auth()
                    .oauth2(this.emissor.emitir("usuario", Set.of("USER")).valor())
                    .get(caminho)
                    .statusCode()
            ).isEqualTo(403);
        }
    }

    @Test
    void deveManterQuantidadesIndependentesPorServico() {
        final var primeiro = criarServico("Troca");
        final var segundo = criarServico("Revisão");
        final var insumo = criarInsumo("Óleo");
        validar(associar(primeiro.id(), insumo.id()), 201);
        final var resposta = requisicao()
            .body(Map.of("insumoId", insumo.id(), "quantidade", 4))
            .post("/servicos/{id}/insumos-obrigatorios", segundo.id());
        validar(resposta, 201);
        assertThat(resposta.as(RespostaInsumoObrigatorio.class).quantidade()).isEqualTo(4);
        assertThat(listar(primeiro.id()).itens().getFirst().quantidade()).isEqualTo(2);
        assertThat(listar(segundo.id()).itens().getFirst().quantidade()).isEqualTo(4);
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
    @ValueSource(strings = { "0", "-1", "1.5", "0.9", "2147483648" })
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
