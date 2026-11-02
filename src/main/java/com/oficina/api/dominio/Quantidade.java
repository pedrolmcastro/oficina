package com.oficina.api.dominio;

public record Quantidade(int valor) {
    public Quantidade {
        if (valor <= 0) {
            throw new DadoInvalidoException("Quantidade deve ser um inteiro positivo");
        }
    }
}
