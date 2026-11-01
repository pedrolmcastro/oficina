package com.oficina.api.dominio.veiculo;

import com.oficina.api.dominio.DadoInvalidoException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record Modelo(
    @Column(name = "modelo", nullable = false, columnDefinition = "text")
    String valor
) {
    public Modelo {
        if (valor.isBlank()) {
            throw new DadoInvalidoException("Modelo não pode estar em branco");
        }
    }
}
