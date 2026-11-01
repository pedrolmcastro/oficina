package com.oficina.api.apresentacao;

import com.oficina.api.aplicacao.RecursoNaoEncontradoException;
import com.oficina.api.aplicacao.cliente.ClienteNaoEncontradoException;
import com.oficina.api.aplicacao.insumo.InsumoNaoEncontradoException;
import com.oficina.api.aplicacao.servico.InsumoObrigatorioNaoEncontradoException;
import com.oficina.api.aplicacao.servico.ServicoNaoEncontradoException;
import com.oficina.api.aplicacao.veiculo.VeiculoNaoEncontradoException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import static org.assertj.core.api.Assertions.assertThat;

class ExceptionHandlersTest {
    @ParameterizedTest
    @MethodSource(value = "recursosNaoEncontrados")
    void deveTraduzirTodosOsRecursosNaoEncontrados(final RecursoNaoEncontradoException erro, final String mensagem) {
        final var resposta = new ExceptionHandlers().naoEncontrado(erro);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).isEqualTo(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, mensagem));
    }

    private static Stream<Arguments> recursosNaoEncontrados() {
        return Stream
            .of(
                Arguments.of(new ClienteNaoEncontradoException(), "Cliente não encontrado"),
                Arguments.of(new VeiculoNaoEncontradoException(), "Veículo não encontrado"),
                Arguments.of(new InsumoNaoEncontradoException(), "Insumo não encontrado"),
                Arguments.of(new ServicoNaoEncontradoException(), "Serviço não encontrado"),
                Arguments
                    .of(
                        new InsumoObrigatorioNaoEncontradoException(),
                        "Associação de insumo obrigatório não encontrada"
                    )
            );
    }
}
