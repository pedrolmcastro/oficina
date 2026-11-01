package com.oficina.api.apresentacao;

import com.oficina.api.aplicacao.cliente.ClienteNaoEncontrado;
import com.oficina.api.aplicacao.cliente.ServicoClientes;
import com.oficina.api.apresentacao.cliente.ControladorClientes;
import com.oficina.api.dominio.cliente.Cliente;
import com.oficina.api.dominio.cliente.Documento;
import com.oficina.api.dominio.cliente.Email;
import com.oficina.api.dominio.cliente.Nome;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ControladorClientes.class)
@AutoConfigureMockMvc(addFilters = false)
class ClientesMvcTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private JsonMapper json;

    @MockitoBean
    private ServicoClientes servico;

    @ParameterizedTest
    @CsvSource(value = {
        "'', maria@example.com, 52998224725",
        "Maria, '', 52998224725",
        "Maria, invalido, 52998224725",
        "Maria, maria@example.com, 11222333000182" })
    void deveRetornar400ParaCadastroInvalido(final String nome, final String email, final String documento)
        throws Exception {
        this.mvc
            .perform(
                post("/clientes")
                    .contentType("application/json")
                    .content(this.json.writeValueAsString(Map.of("nome", nome, "email", email, "documento", documento)))
            )
            .andExpect(status().isBadRequest());
        verifyNoInteractions(this.servico);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{",
        "{}",
        "{\"nome\":\"Maria\"}",
        "{\"nome\":null,\"email\":\"maria@example.com\",\"documento\":\"52998224725\"}",
        "{\"nome\":\"Maria\",\"email\":null,\"documento\":\"52998224725\"}",
        "{\"nome\":\"Maria\",\"email\":\"maria@example.com\",\"documento\":null}",
        "{\"nome\":\"Maria\",\"email\":\"maria@example.com\",\"documento\":\"52998224725\",\"extra\":1}" })
    void deveRetornar400ParaPayloadDeCadastroInvalido(final String json) throws Exception {
        this.mvc
            .perform(post("/clientes").contentType("application/json").content(json))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(content().json("{\"status\":400,\"detail\":\"Requisição inválida\"}"));
        verifyNoInteractions(this.servico);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "52998224726" })
    void deveRetornar400ParaConsultaComDocumentoInvalido(final String documento) throws Exception {
        this.mvc.perform(get("/clientes").param("documento", documento)).andExpect(status().isBadRequest());
        verifyNoInteractions(this.servico);
    }

    @Test
    void deveExigirParametroDocumento() throws Exception {
        this.mvc.perform(get("/clientes")).andExpect(status().isBadRequest());
        verifyNoInteractions(this.servico);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{",
        "{}",
        "{\"nome\":null,\"email\":null,\"documento\":null}",
        "{\"nome\":\" \"}",
        "{\"email\":\"\"}",
        "{\"email\":\"invalido\"}",
        "{\"documento\":\"123\"}",
        "{\"documento\":\"52998224726\"}",
        "{\"campoDesconhecido\":\"valor\"}" })
    void deveRejeitarAtualizacaoInvalidaAntesDeChamarServico(final String json) throws Exception {
        this.mvc
            .perform(patch("/clientes/{id}", UUID.randomUUID()).contentType("application/json").content(json))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(this.servico);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "application/json, '{\"email\":\"novo@example.com\"}'",
        "application/merge-patch+json, '{\"email\":\"novo@example.com\"}'",
        "application/json, '{\"email\":\"novo@example.com\",\"nome\":null,\"documento\":null}'",
        "application/merge-patch+json, '{\"email\":\"novo@example.com\",\"nome\":null,\"documento\":null}'" })
    void deveConverterCamposOmitidosENulos(final String formato, final String payload) throws Exception {
        final var cliente = new Cliente(new Nome("Maria"), new Email("novo@example.com"), Documento.de("52998224725"));
        when(this.servico.atualizar(cliente.id(), null, new Email("novo@example.com"), null)).thenReturn(cliente);

        this.mvc
            .perform(patch("/clientes/{id}", cliente.id()).contentType(formato).content(payload))
            .andExpect(status().isOk());

        verify(this.servico).atualizar(cliente.id(), null, new Email("novo@example.com"), null);
    }

    @Test
    void deveTraduzirClienteNaoEncontrado() throws Exception {
        final var id = UUID.randomUUID();
        when(this.servico.buscar(id)).thenThrow(new ClienteNaoEncontrado());

        this.mvc
            .perform(get("/clientes/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(content().json("{\"status\":404,\"detail\":\"Cliente não encontrado\"}"));
    }
}
