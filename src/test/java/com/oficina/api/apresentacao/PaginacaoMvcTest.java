package com.oficina.api.apresentacao;

import com.oficina.api.aplicacao.insumo.ServicoInsumos;
import com.oficina.api.aplicacao.servico.ServicoServicos;
import com.oficina.api.aplicacao.veiculo.ServicoVeiculos;
import com.oficina.api.apresentacao.insumo.ControladorInsumos;
import com.oficina.api.apresentacao.servico.ControladorInsumosObrigatorios;
import com.oficina.api.apresentacao.servico.ControladorServicos;
import com.oficina.api.apresentacao.veiculo.ControladorVeiculos;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
    ControladorInsumos.class,
    ControladorServicos.class,
    ControladorInsumosObrigatorios.class,
    ControladorVeiculos.class })
@AutoConfigureMockMvc(addFilters = false)
class PaginacaoMvcTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private ServicoInsumos insumos;
    @MockitoBean
    private ServicoServicos servicos;
    @MockitoBean
    private ServicoVeiculos veiculos;

    @ParameterizedTest
    @MethodSource(value = "consultasInvalidas")
    void deveRejeitarPaginacaoInvalida(final String caminho, final String consulta) throws Exception {
        this.mvc.perform(get(caminho + "?" + consulta)).andExpect(status().isBadRequest());
        verifyNoInteractions(this.insumos, this.servicos, this.veiculos);
    }

    private static Stream<Arguments> consultasInvalidas() {
        return Stream
            .of(
                "/insumos",
                "/servicos",
                "/veiculos",
                "/servicos/00000000-0000-0000-0000-000000000001/insumos-obrigatorios"
            )
            .flatMap(
                caminho -> Stream
                    .of("first=0", "first=26", "first=abc", "after=invalido")
                    .map(consulta -> Arguments.of(caminho, consulta))
            );
    }
}
