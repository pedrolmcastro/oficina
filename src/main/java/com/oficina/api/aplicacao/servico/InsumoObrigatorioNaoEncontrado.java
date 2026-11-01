package com.oficina.api.aplicacao.servico;

public final class InsumoObrigatorioNaoEncontrado extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InsumoObrigatorioNaoEncontrado() {
        super("Associação de insumo obrigatório não encontrada");
    }
}
