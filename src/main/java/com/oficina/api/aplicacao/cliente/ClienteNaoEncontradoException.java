package com.oficina.api.aplicacao.cliente;

import com.oficina.api.aplicacao.RecursoNaoEncontradoException;

public final class ClienteNaoEncontradoException extends RecursoNaoEncontradoException {
    private static final long serialVersionUID = 1L;

    public ClienteNaoEncontradoException() {
        super("Cliente não encontrado");
    }
}
