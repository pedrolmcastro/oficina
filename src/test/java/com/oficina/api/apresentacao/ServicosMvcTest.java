package com.oficina.api.apresentacao;

import com.oficina.api.aplicacao.servico.ServicoServicos;
import com.oficina.api.apresentacao.servico.ControladorInsumosObrigatorios;
import com.oficina.api.apresentacao.servico.ControladorServicos;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { ControladorServicos.class, ControladorInsumosObrigatorios.class })
@AutoConfigureMockMvc(addFilters = false)
class ServicosMvcTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private ServicoServicos servico;

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"nome\":\"Troca\",\"precoVenda\":10}",
        "{\"nome\":\"Troca\",\"precoVenda\":10,\"requerDiagnostico\":null}",
        "{\"nome\":\"Óleo\"}",
        "{\"precoVenda\":10}",
        "{\"nome\":\" \",\"precoVenda\":10,\"requerDiagnostico\":false}" })
    void deveRejeitarCadastroSemCamposObrigatoriosValidos(final String corpo) throws Exception {
        this.mvc
            .perform(post("/servicos").contentType("application/json").content(corpo))
            .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}",
        "{\"nome\":null,\"precoVenda\":null}",
        "{\"nome\":\" \"}",
        "{\"precoVenda\":0.001}" })
    void deveRejeitarAtualizacaoSemCamposOuComValoresInvalidos(final String corpo) throws Exception {
        this.mvc
            .perform(patch("/servicos/{id}", UUID.randomUUID()).contentType("application/json").content(corpo))
            .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = { "{}", "{\"insumoId\":null}", "{\"insumoId\":\"invalido\"}", "{\"insumoId\":\"\"}" })
    void deveRejeitarAssociacaoSemInsumoValido(final String corpo) throws Exception {
        this.mvc
            .perform(
                post("/servicos/{id}/insumos-obrigatorios", UUID.randomUUID())
                    .contentType("application/json")
                    .content(corpo)
            )
            .andExpect(status().isBadRequest());
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
    }

    @Test
    void deveRejeitarSubstituicaoViaPut() throws Exception {
        this.mvc.perform(put("/servicos/{id}", UUID.randomUUID())).andExpect(status().isMethodNotAllowed());
    }

    @ParameterizedTest
    @ValueSource(strings = { "first=0", "first=26", "after=invalido" })
    void deveRejeitarPaginacaoInvalida(final String consulta) throws Exception {
        this.mvc.perform(get("/servicos?" + consulta)).andExpect(status().isBadRequest());
        this.mvc
            .perform(get("/servicos/{id}/insumos-obrigatorios?" + consulta, UUID.randomUUID()))
            .andExpect(status().isBadRequest());
    }
}
