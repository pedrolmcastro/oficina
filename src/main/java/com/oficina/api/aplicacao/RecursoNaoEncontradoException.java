package com.oficina.api.aplicacao;

public abstract class RecursoNaoEncontradoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    protected RecursoNaoEncontradoException(final String mensagem) {
        super(mensagem);
    }
}
