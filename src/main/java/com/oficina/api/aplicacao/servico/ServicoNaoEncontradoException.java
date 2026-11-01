package com.oficina.api.aplicacao.servico;

import com.oficina.api.aplicacao.RecursoNaoEncontradoException;

public final class ServicoNaoEncontradoException extends RecursoNaoEncontradoException {
    private static final long serialVersionUID = 1L;

    public ServicoNaoEncontradoException() {
        super("Serviço não encontrado");
    }
}
