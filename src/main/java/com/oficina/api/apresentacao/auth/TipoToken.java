package com.oficina.api.apresentacao.auth;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TipoToken {
    BEARER("Bearer");

    private final String valor;

    TipoToken(final String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String valor() {
        return this.valor;
    }
}
