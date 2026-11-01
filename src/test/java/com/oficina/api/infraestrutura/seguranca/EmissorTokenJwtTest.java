package com.oficina.api.infraestrutura.seguranca;

import java.time.Duration;
import java.util.Set;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import static com.oficina.api.infraestrutura.seguranca.ConfiguracaoSeguranca.TAMANHO_MINIMO_SEGREDO_BYTES;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmissorTokenJwtTest {
    @Test
    void devePropagarFalhaNaAssinatura() {
        final var encoder = mock(JwtEncoder.class);
        final var falha = new JwtEncodingException("Falha ao assinar");
        when(encoder.encode(any())).thenThrow(falha);
        final var emissor = new EmissorTokenJwt(encoder, propriedades(Duration.ofMinutes(30)));

        assertThatThrownBy(() -> emissor.emitir("admin", Set.of("ADMIN"))).isSameAs(falha);
    }

    @ParameterizedTest
    @ValueSource(longs = { 0, -1, -2 })
    void deveRejeitarValidadeNaoPositiva(final long segundos) {
        final var encoder = new ConfiguracaoSeguranca()
            .jwtEncoder(new SecretKeySpec(new byte[TAMANHO_MINIMO_SEGREDO_BYTES], "HmacSHA256"));

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new EmissorTokenJwt(encoder, propriedades(Duration.ofSeconds(segundos))))
            .withMessage("A validade do JWT deve ser positiva");
    }

    private static PropriedadesSeguranca propriedades(final Duration validade) {
        return new PropriedadesSeguranca(
            new PropriedadesSeguranca.Admin("admin", "hash"),
            new PropriedadesSeguranca.Jwt("segredo", validade)
        );
    }
}
