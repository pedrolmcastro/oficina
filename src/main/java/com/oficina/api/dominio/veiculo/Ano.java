package com.oficina.api.dominio.veiculo;

import com.oficina.api.dominio.DadoInvalidoException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Year;
import java.time.ZoneId;

@Embeddable
public record Ano(
    @Column(name = "ano", nullable = false)
    int valor
) {
    @SuppressWarnings(value = "TimeInStaticInitializer") // Limite fixado no carregamento da classe, por definição.
    public static final int ANO_MAXIMO = Year.now(ZoneId.of("America/Sao_Paulo")).getValue() + 1;

    public Ano {
        if (valor < 1886 || valor > ANO_MAXIMO) {
            throw new DadoInvalidoException("Ano deve estar entre 1886 e " + ANO_MAXIMO);
        }
    }
}
