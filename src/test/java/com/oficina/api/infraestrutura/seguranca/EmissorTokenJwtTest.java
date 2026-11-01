package com.oficina.api.infraestrutura.seguranca;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

class EmissorTokenJwtTest {
    @Test
    void deveEmitirTokenComIdentidadePerfisEValidade() {
        final var configuracao = new ConfiguracaoSeguranca();
        final var chave = new SecretKeySpec(new byte[ConfiguracaoSeguranca.TAMANHO_MINIMO_SEGREDO_BYTES], "HmacSHA256");
        final var emissor = new EmissorTokenJwt(configuracao.jwtEncoder(chave), propriedades(Duration.ofMinutes(30)));
        final var antes = Instant.now();

        final var token = emissor.emitir("maria", Set.of("ADMIN"));
        final var jwt = configuracao.jwtDecoder(chave).decode(token.valor());

        assertThat(jwt.getSubject()).isEqualTo("maria");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
        assertThat(jwt.getIssuedAt()).isBetween(antes.truncatedTo(ChronoUnit.SECONDS), Instant.now());
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(30));
        assertThat(token.validadeEmSegundos()).isEqualTo(1800);
    }

    @ParameterizedTest
    @ValueSource(longs = { 0, -1 })
    void deveRejeitarValidadeNaoPositiva(final long segundos) {
        final var encoder = mock(JwtEncoder.class);

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
