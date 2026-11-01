package com.oficina.api.apresentacao;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.assertThat;

class ExceptionHandlersTest {
    @ParameterizedTest
    @MethodSource(value = "falhasInternas")
    void deveSensurarFalhaInterna(final Exception erro) {
        final var resposta = new ExceptionHandlers().falhaInterna(erro);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getHeaders().getCacheControl()).isEqualTo("no-store");

        assertThat(resposta.getBody())
            .isEqualTo(
                ProblemDetail
                    .forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível processar a requisição")
            );
    }

    private static Stream<Exception> falhasInternas() {
        return Stream
            .of(
                new IllegalStateException("Detalhes internos confidenciais"),
                new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Detalhes internos confidenciais")
            );
    }
}
