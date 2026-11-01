package com.oficina.api.infraestrutura.seguranca;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(value = "app.seguranca")
public record PropriedadesSeguranca(@Valid @NotNull Admin admin, @Valid @NotNull Jwt jwt) {
    public static record Admin(@NotBlank String nomeUsuario, @NotBlank String hashSenha) {}

    public static record Jwt(@NotBlank String segredo, @NotNull Duration validade) {}
}
