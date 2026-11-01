package com.oficina.api.dominio.servico;

import com.oficina.api.dominio.TextoNormalizado;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

@Embeddable
public record Nome(
    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "nome", nullable = false, columnDefinition = "text"))
    TextoNormalizado texto
) {}
