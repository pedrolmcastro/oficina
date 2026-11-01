package com.oficina.api.aplicacao.veiculo;

public final class VeiculoNaoEncontrado extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public VeiculoNaoEncontrado() {
        super("Veículo não encontrado");
    }
}
