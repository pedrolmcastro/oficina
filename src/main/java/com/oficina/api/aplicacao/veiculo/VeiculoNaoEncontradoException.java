package com.oficina.api.aplicacao.veiculo;

import com.oficina.api.aplicacao.RecursoNaoEncontradoException;

public final class VeiculoNaoEncontradoException extends RecursoNaoEncontradoException {
    private static final long serialVersionUID = 1L;

    public VeiculoNaoEncontradoException() {
        super("Veículo não encontrado");
    }
}
