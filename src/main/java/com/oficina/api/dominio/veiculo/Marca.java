package com.oficina.api.dominio.veiculo;

import com.oficina.api.dominio.TextoNormalizado;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

@Embeddable
public record Marca(
    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "marca", nullable = false, columnDefinition = "text"))
    TextoNormalizado texto
) {}
