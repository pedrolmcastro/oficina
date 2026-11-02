package com.oficina.api.dominio.ordemservico;

public final class ConflitoOrdemServicoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ConflitoOrdemServicoException(final String mensagem) {
        super(mensagem);
    }
}
