package com.oficina.api.aplicacao.servico;

public final class ServicoNaoEncontrado extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ServicoNaoEncontrado() {
        super("Serviço não encontrado");
    }
}
