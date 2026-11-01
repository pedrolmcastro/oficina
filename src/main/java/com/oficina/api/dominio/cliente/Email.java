package com.oficina.api.dominio.cliente;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;

@Embeddable
public record Email(
    @Column(name = "email", nullable = false, columnDefinition = "text")
    @NotBlank
    @jakarta.validation.constraints.Email
    String valor
) {
    private static final ValidatorFactory FABRICA_VALIDACAO = Validation.buildDefaultValidatorFactory();

    public Email {
        if (!FABRICA_VALIDACAO.getValidator().validateValue(Email.class, "valor", valor).isEmpty()) {
            throw new IllegalArgumentException("Email inválido");
        }
    }
}
