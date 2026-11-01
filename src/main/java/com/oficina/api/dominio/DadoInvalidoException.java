package com.oficina.api.dominio;

public final class DadoInvalidoException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public DadoInvalidoException(final String mensagem) {
        super(mensagem);
    }
}
