package com.oficina.api.dominio.insumo;

import com.oficina.api.dominio.DadoInvalidoException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record Nome(
    @Column(name = "nome", nullable = false, columnDefinition = "text")
    String valor
) {
    public Nome {
        if (valor.isBlank()) {
            throw new DadoInvalidoException("Nome não pode estar em branco");
        }
    }
}
