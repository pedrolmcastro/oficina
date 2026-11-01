package com.oficina.api.aplicacao.servico;

import com.oficina.api.aplicacao.RecursoNaoEncontradoException;

public final class InsumoObrigatorioNaoEncontradoException extends RecursoNaoEncontradoException {
    private static final long serialVersionUID = 1L;

    public InsumoObrigatorioNaoEncontradoException() {
        super("Associação de insumo obrigatório não encontrada");
    }
}
