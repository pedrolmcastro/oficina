package com.oficina.api.aplicacao.insumo;

import com.oficina.api.aplicacao.RecursoNaoEncontradoException;

public final class InsumoNaoEncontradoException extends RecursoNaoEncontradoException {
    private static final long serialVersionUID = 1L;

    public InsumoNaoEncontradoException() {
        super("Insumo não encontrado");
    }
}
