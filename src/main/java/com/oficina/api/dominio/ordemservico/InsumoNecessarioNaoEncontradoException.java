package com.oficina.api.dominio.ordemservico;

import com.oficina.api.dominio.RecursoNaoEncontradoException;

public final class InsumoNecessarioNaoEncontradoException extends RecursoNaoEncontradoException {
    private static final long serialVersionUID = 1L;

    public InsumoNecessarioNaoEncontradoException() {
        super("Insumo necessário não encontrado nesta OS");
    }
}
