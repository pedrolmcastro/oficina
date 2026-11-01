package com.oficina.api.apresentacao;

import com.oficina.api.aplicacao.Pagina;
import com.oficina.api.aplicacao.veiculo.ServicoVeiculos;
import com.oficina.api.apresentacao.veiculo.ControladorVeiculos;
import com.oficina.api.apresentacao.veiculo.RespostaVeiculo;
import com.oficina.api.dominio.TextoNormalizado;
import com.oficina.api.dominio.veiculo.Ano;
import com.oficina.api.dominio.veiculo.Marca;
import com.oficina.api.dominio.veiculo.Modelo;
import com.oficina.api.dominio.veiculo.Placa;
import com.oficina.api.dominio.veiculo.Veiculo;
import java.util.List;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ControladorVeiculos.class)
@AutoConfigureMockMvc(addFilters = false)
class VeiculosMvcTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private ServicoVeiculos servico;

    @Autowired
    private JsonMapper json;

    @ParameterizedTest
    @ValueSource(strings = {
        "{",
        "{}",
        "{\"placa\":\"ABC1234\"}",
        "{\"placa\":null,\"modelo\":\"Corolla\",\"marca\":\"Toyota\",\"ano\":2024"
            + ",\"clienteId\":\"00000000-0000-0000-0000-000000000001\"}",
        "{\"placa\":\"ABC1234\",\"modelo\":null,\"marca\":\"Toyota\",\"ano\":2024"
            + ",\"clienteId\":\"00000000-0000-0000-0000-000000000001\"}",
        "{\"placa\":\"ABC1234\",\"modelo\":\"Corolla\",\"marca\":null,\"ano\":2024"
            + ",\"clienteId\":\"00000000-0000-0000-0000-000000000001\"}",
        "{\"placa\":\"ABC1234\",\"modelo\":\"Corolla\",\"marca\":\"Toyota\",\"ano\":null"
            + ",\"clienteId\":\"00000000-0000-0000-0000-000000000001\"}",
        "{\"placa\":\"ABC1234\",\"modelo\":\"Corolla\",\"marca\":\"Toyota\",\"ano\":2024,\"clienteId\":null}",
        "{\"placa\":\"ABC1234\",\"modelo\":\"Corolla\",\"marca\":\"Toyota\",\"ano\":2024"
            + ",\"clienteId\":\"00000000-0000-0000-0000-000000000001\",\"extra\":1}" })
    void deveRejeitarPayloadDeCadastroInvalido(final String payload) throws Exception {
        this.mvc
            .perform(post("/veiculos").contentType("application/json").content(payload))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(content().json("{\"status\":400,\"detail\":\"Requisição inválida\"}"));
        verifyNoInteractions(this.servico);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "ABC123, Corolla, Toyota, 2024",
        "ABC1234, '', Toyota, 2024",
        "ABC1234, Corolla, ' ', 2024",
        "ABC1234, Corolla, Toyota, 0" })
    void deveRejeitarDadosDeCadastroInvalidos(
        final String placa,
        final String modelo,
        final String marca,
        final int ano
    ) throws Exception {
        final var payload = this.json
            .writeValueAsString(
                Map.of("placa", placa, "modelo", modelo, "marca", marca, "ano", ano, "clienteId", UUID.randomUUID())
            );
        this.mvc
            .perform(post("/veiculos").contentType("application/json").content(payload))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(this.servico);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{",
        "{}",
        "{\"placa\":null,\"modelo\":null,\"marca\":null,\"ano\":null,\"clienteId\":null}",
        "{\"placa\":\"123\"}",
        "{\"placa\":\"ABC-1234\"}",
        "{\"modelo\":\" \"}",
        "{\"marca\":\"\"}",
        "{\"ano\":0}",
        "{\"clienteId\":\"invalido\"}",
        "{\"extra\":1}" })
    void deveRejeitarAtualizacaoInvalida(final String payload) throws Exception {
        this.mvc
            .perform(patch("/veiculos/{id}", UUID.randomUUID()).contentType("application/json").content(payload))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(this.servico);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "application/json, '{\"modelo\":\"Civic\"}'",
        "application/merge-patch+json, '{\"modelo\":\"Civic\"}'",
        "application/json, '{\"modelo\":\"Civic\",\"marca\":null,\"placa\":null,\"ano\":null,\"clienteId\":null}'",
        "application/merge-patch+json, '{\"modelo\":\"Civic\",\"marca\":null,\"placa\":null,\"ano\":null"
            + ",\"clienteId\":null}'" })
    void deveConverterCamposOmitidosENulos(final String formato, final String payload) throws Exception {
        final var veiculo = new Veiculo(
            new Placa("ABC1234"),
            new Modelo(new TextoNormalizado("Civic")),
            new Marca(new TextoNormalizado("Toyota")),
            new Ano(2024),
            UUID.randomUUID()
        );
        when(this.servico.atualizar(veiculo.id(), new Modelo(new TextoNormalizado("Civic")), null, null, null, null))
            .thenReturn(veiculo);
        this.mvc
            .perform(patch("/veiculos/{id}", veiculo.id()).contentType(formato).content(payload))
            .andExpect(status().isOk())
            .andExpect(
                content()
                    .json(
                        this.json
                            .writeValueAsString(
                                new RespostaVeiculo(
                                    veiculo.id(),
                                    "ABC1234",
                                    "Civic",
                                    "Toyota",
                                    2024,
                                    veiculo.clienteId()
                                )
                            )
                    )
            );
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "ABC12D3", "ABC-1234" })
    void deveRejeitarConsultaComPlacaInvalida(final String placa) throws Exception {
        this.mvc.perform(get("/veiculos").param("placa", placa)).andExpect(status().isBadRequest());
        verifyNoInteractions(this.servico);
    }

    @Test
    void deveValidarId() throws Exception {
        this.mvc.perform(get("/veiculos/invalido")).andExpect(status().isBadRequest());
        verifyNoInteractions(this.servico);
    }

    @Test
    void deveRejeitarClienteInvalido() throws Exception {
        this.mvc.perform(get("/veiculos").param("cliente", "invalido")).andExpect(status().isBadRequest());
        verifyNoInteractions(this.servico);
    }

    @Test
    void deveListarPorPlacaComTamanhoPadrao() throws Exception {
        when(this.servico.listar(new Placa("ABC1234"), null, null, new Pagina.Limite(25)))
            .thenReturn(new Pagina<>(List.of(), null));
        this.mvc
            .perform(get("/veiculos").param("placa", "ABC1234"))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"itens\":[],\"nextCursor\":null}"));
    }

    @Test
    void deveConverterFiltrosEPaginacaoParaRespostaHttp() throws Exception {
        final var cliente = UUID.randomUUID();
        final var after = UUID.randomUUID();
        final var veiculo = new Veiculo(
            new Placa("ABC1234"),
            new Modelo(new TextoNormalizado("Corolla")),
            new Marca(new TextoNormalizado("Toyota")),
            new Ano(2024),
            cliente
        );
        when(this.servico.listar(new Placa("ABC1234"), cliente, after, new Pagina.Limite(1)))
            .thenReturn(new Pagina<>(List.of(veiculo), veiculo.id()));
        this.mvc
            .perform(
                get("/veiculos")
                    .param("placa", "ABC1234")
                    .param("cliente", cliente.toString())
                    .param("after", after.toString())
                    .param("first", "1")
            )
            .andExpect(status().isOk())
            .andExpect(
                content()
                    .json(
                        ("{\"itens\":[{\"id\":\"%s\",\"placa\":\"ABC1234\",\"modelo\":\"Corolla\","
                            + "\"marca\":\"Toyota\",\"ano\":2024,\"clienteId\":\"%s\"}],\"nextCursor\":\"%s\"}")
                            .formatted(veiculo.id(), cliente, veiculo.id())
                    )
            );
    }
}
