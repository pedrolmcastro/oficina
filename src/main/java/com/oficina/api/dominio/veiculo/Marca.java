package com.oficina.api.dominio.veiculo;

import com.oficina.api.dominio.DadoInvalidoException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record Marca(
    @Column(name = "marca", nullable = false, columnDefinition = "text")
    String valor
) {
    public Marca {
        if (valor.isBlank()) {
            throw new DadoInvalidoException("Marca não pode estar em branco");
        }
    }
}
