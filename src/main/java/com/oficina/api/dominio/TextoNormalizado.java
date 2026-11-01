package com.oficina.api.dominio;

import jakarta.persistence.Embeddable;

@Embeddable
public record TextoNormalizado(String valor) {
    public TextoNormalizado {
        if (valor.isBlank()) {
            throw new DadoInvalidoException("Texto não pode estar em branco");
        }
        if (!valor.equals(valor.trim())) {
            throw new DadoInvalidoException("Texto não pode conter espaços no início ou no fim");
        }
    }
}
