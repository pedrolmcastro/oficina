package com.oficina.api.aplicacao.insumo;

public final class InsumoNaoEncontrado extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InsumoNaoEncontrado() {
        super("Insumo não encontrado");
    }
}
