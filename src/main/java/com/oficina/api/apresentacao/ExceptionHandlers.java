package com.oficina.api.apresentacao;

import com.oficina.api.aplicacao.RecursoNaoEncontradoException;
import com.oficina.api.dominio.DadoInvalidoException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ExceptionHandlers {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandlers.class);
    private static final String REQUISICAO_INVALIDA = "Requisição inválida";

    @ExceptionHandler(value = DadoInvalidoException.class)
    public ResponseEntity<ProblemDetail> dadosInvalidos(
        final DadoInvalidoException erro // NOSONAR S3242: Prefer most specific type for handler
    ) {
        return buildRespostaFalha(
            HttpStatus.BAD_REQUEST,
            Objects.requireNonNullElse(erro.getMessage(), REQUISICAO_INVALIDA)
        );
    }

    @ExceptionHandler(value = { HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    public ResponseEntity<ProblemDetail> requisicaoInvalida() {
        return buildRespostaFalha(HttpStatus.BAD_REQUEST, REQUISICAO_INVALIDA);
    }

    @ExceptionHandler(value = RecursoNaoEncontradoException.class)
    public ResponseEntity<ProblemDetail> naoEncontrado(final Throwable erro) {
        return buildRespostaFalha(HttpStatus.NOT_FOUND, Objects.requireNonNull(erro.getMessage()));
    }

    @ExceptionHandler(value = DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> conflito() {
        return buildRespostaFalha(HttpStatus.CONFLICT, "Cadastro conflita com os dados existentes");
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ProblemDetail> falhaInterna(final Exception erro) {
        if (erro instanceof ErrorResponse resposta && resposta.getStatusCode().is4xxClientError()) {
            return buildRespostaFalha(HttpStatus.valueOf(resposta.getStatusCode().value()), REQUISICAO_INVALIDA);
        }
        LOGGER.error("Falha ao processar requisição", erro);
        return buildRespostaFalha(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível processar a requisição");
    }

    private static ResponseEntity<ProblemDetail> buildRespostaFalha(final HttpStatus status, final String detalhe) {
        return RespostasHttp.status(status).body(ProblemDetail.forStatusAndDetail(status, detalhe));
    }
}
