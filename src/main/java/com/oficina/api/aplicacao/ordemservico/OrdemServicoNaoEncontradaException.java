package com.oficina.api.aplicacao.ordemservico;

import com.oficina.api.dominio.RecursoNaoEncontradoException;

public final class OrdemServicoNaoEncontradaException extends RecursoNaoEncontradoException {
    private static final long serialVersionUID = 1L;

    public OrdemServicoNaoEncontradaException() {
        super("Ordem de serviço não encontrada");
    }
}
