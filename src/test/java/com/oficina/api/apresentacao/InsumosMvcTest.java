package com.oficina.api.apresentacao;

import com.oficina.api.aplicacao.insumo.ServicoInsumos;
import com.oficina.api.apresentacao.insumo.ControladorInsumos;
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

@WebMvcTest(controllers = ControladorInsumos.class)
@AutoConfigureMockMvc(addFilters = false)
class InsumosMvcTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private ServicoInsumos servico;

    @ParameterizedTest
    @ValueSource(strings = { "{}", "{\"nome\":\"Óleo\"}", "{\"precoVenda\":10}", "{\"nome\":\" \",\"precoVenda\":10}" })
    void deveRejeitarCadastroSemNomeOuPrecoValido(final String corpo) throws Exception {
        this.mvc
            .perform(post("/insumos").contentType("application/json").content(corpo))
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
            .perform(patch("/insumos/{id}", UUID.randomUUID()).contentType("application/json").content(corpo))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deveRejeitarSubstituicaoViaPut() throws Exception {
        this.mvc.perform(put("/insumos/{id}", UUID.randomUUID())).andExpect(status().isMethodNotAllowed());
    }

    @ParameterizedTest
    @ValueSource(strings = { "first=0", "first=26", "after=invalido" })
    void deveRejeitarPaginacaoInvalida(final String consulta) throws Exception {
        this.mvc.perform(get("/insumos?" + consulta)).andExpect(status().isBadRequest());
    }
}
