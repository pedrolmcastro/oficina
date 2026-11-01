package com.oficina.api.aplicacao.cliente;

public final class ClienteNaoEncontrado extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ClienteNaoEncontrado() {
        super("Cliente não encontrado");
    }
}
