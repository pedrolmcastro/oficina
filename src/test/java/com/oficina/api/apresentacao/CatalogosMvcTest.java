package com.oficina.api.apresentacao;

import com.oficina.api.aplicacao.insumo.ServicoInsumos;
import com.oficina.api.aplicacao.servico.ServicoServicos;
import com.oficina.api.apresentacao.insumo.ControladorInsumos;
import com.oficina.api.apresentacao.servico.ControladorInsumosObrigatorios;
import com.oficina.api.apresentacao.servico.ControladorServicos;
import com.oficina.api.dominio.Preco;
import com.oficina.api.dominio.TextoNormalizado;
import com.oficina.api.dominio.servico.Nome;
import com.oficina.api.dominio.servico.PrecoVenda;
import com.oficina.api.dominio.servico.Servico;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { ControladorInsumos.class, ControladorServicos.class, ControladorInsumosObrigatorios.class })
@AutoConfigureMockMvc(addFilters = false)
class CatalogosMvcTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private ServicoInsumos insumos;
    @MockitoBean
    private ServicoServicos servicos;

    @ParameterizedTest
    @MethodSource(value = "cadastrosInvalidos")
    void deveRejeitarCadastroInvalido(final String caminho, final String corpo) throws Exception {
        this.mvc
            .perform(post(caminho).contentType("application/json").content(corpo))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(this.insumos, this.servicos);
    }

    @ParameterizedTest
    @MethodSource(value = "atualizacoesInvalidas")
    void deveRejeitarAtualizacaoInvalida(final String caminho, final String corpo) throws Exception {
        this.mvc
            .perform(patch(caminho + "/{id}", UUID.randomUUID()).contentType("application/json").content(corpo))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(this.insumos, this.servicos);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/insumos", "/servicos" })
    void deveRejeitarSubstituicaoViaPut(final String caminho) throws Exception {
        this.mvc.perform(put(caminho + "/{id}", UUID.randomUUID())).andExpect(status().isMethodNotAllowed());
    }

    @ParameterizedTest
    @ValueSource(strings = { "{}", "{\"insumoId\":null}", "{\"insumoId\":\"invalido\"}" })
    void deveRejeitarAssociacaoSemInsumoValido(final String corpo) throws Exception {
        this.mvc
            .perform(
                post("/servicos/{id}/insumos-obrigatorios", UUID.randomUUID())
                    .contentType("application/json")
                    .content(corpo)
            )
            .andExpect(status().isBadRequest());
        verifyNoInteractions(this.servicos);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", ",\"quantidade\":null" })
    void deveExigirQuantidadeNaAssociacao(final String quantidade) throws Exception {
        final var json = "{\"insumoId\":\"%s\"%s}".formatted(UUID.randomUUID(), quantidade);
        this.mvc
            .perform(
                post("/servicos/{id}/insumos-obrigatorios", UUID.randomUUID())
                    .contentType("application/json")
                    .content(json)
            )
            .andExpect(status().isBadRequest());
        verifyNoInteractions(this.servicos);
    }

    @Test
    void deveRejeitarInsumosNoPatchDoServico() throws Exception {
        this.mvc
            .perform(
                patch("/servicos/{id}", UUID.randomUUID())
                    .contentType("application/json")
                    .content("{\"nome\":\"Troca\",\"insumosObrigatorios\":[]}")
            )
            .andExpect(status().isBadRequest());
        verifyNoInteractions(this.servicos);
    }

    @Test
    void deveAceitarAtualizacaoSomenteDaExigenciaDeDiagnostico() throws Exception {
        final var servico = new Servico(
            new Nome(new TextoNormalizado("Troca de óleo")),
            new PrecoVenda(new Preco(new BigDecimal("100.00"))),
            false
        );
        when(this.servicos.atualizar(servico.id(), null, null, false)).thenReturn(servico);
        this.mvc
            .perform(
                patch("/servicos/{id}", servico.id())
                    .contentType("application/json")
                    .content("{\"requerDiagnostico\":false}")
            )
            .andExpect(status().isOk())
            .andExpect(
                content()
                    .json(
                        ("{\"id\":\"%s\",\"nome\":\"Troca de óleo\","
                            + "\"precoVenda\":100.00,\"requerDiagnostico\":false}").formatted(servico.id())
                    )
            );
    }

    private static Stream<Arguments> cadastrosInvalidos() {
        return Stream
            .concat(
                Stream
                    .of("{\"precoVenda\":10}", "{\"nome\":\"Óleo\"}", "{\"nome\":\" \",\"precoVenda\":10}")
                    .map(corpo -> Arguments.of("/insumos", corpo)),
                Stream
                    .of(
                        "{\"nome\":\"Troca\",\"precoVenda\":10}",
                        "{\"precoVenda\":10,\"requerDiagnostico\":false}",
                        "{\"nome\":\"Troca\",\"requerDiagnostico\":false}",
                        "{\"nome\":\" \",\"precoVenda\":10,\"requerDiagnostico\":false}"
                    )
                    .map(corpo -> Arguments.of("/servicos", corpo))
            );
    }

    private static Stream<Arguments> atualizacoesInvalidas() {
        return Stream
            .of("/insumos", "/servicos")
            .flatMap(
                caminho -> Stream
                    .of("{}", "{\"nome\":null,\"precoVenda\":null}", "{\"nome\":\" \"}", "{\"precoVenda\":0.001}")
                    .map(corpo -> Arguments.of(caminho, corpo))
            );
    }
}
