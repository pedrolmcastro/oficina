package com.oficina.api.infraestrutura.seguranca;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import static com.oficina.api.infraestrutura.seguranca.ConfiguracaoSeguranca.TAMANHO_MINIMO_SEGREDO_BYTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfiguracaoSegurancaTest {
    @ParameterizedTest
    @ValueSource(ints = { 0, 1, TAMANHO_MINIMO_SEGREDO_BYTES / 2, TAMANHO_MINIMO_SEGREDO_BYTES - 1 })
    void deveRejeitarSegredoCurto(final int tamanho) {
        final var propriedades = propriedades(new byte[tamanho]);

        final var configuracao = new ConfiguracaoSeguranca();
        assertThatIllegalArgumentException()
            .isThrownBy(() -> configuracao.secretKey(propriedades))
            .withMessageContaining("pelo menos " + TAMANHO_MINIMO_SEGREDO_BYTES + " bytes");
    }

    @Test
    void deveAceitarSegredoComTamanhoMinimo() {
        final var bytes = new byte[TAMANHO_MINIMO_SEGREDO_BYTES];
        final var chave = new ConfiguracaoSeguranca().secretKey(propriedades(bytes));

        assertThat(chave.getEncoded()).containsExactly(bytes);
        assertThat(chave.getAlgorithm()).isEqualTo("HmacSHA256");
    }

    @Test
    void deveRejeitarAssinaturaDiferente() {
        final var configuracao = new ConfiguracaoSeguranca();
        final var chave = configuracao.secretKey(propriedades(new byte[TAMANHO_MINIMO_SEGREDO_BYTES]));
        final var outrosBytes = new byte[TAMANHO_MINIMO_SEGREDO_BYTES];
        outrosBytes[0] = 1;
        final var outraChave = new SecretKeySpec(outrosBytes, "HmacSHA256");
        final var outroEncoder = NimbusJwtEncoder.withSecretKey(outraChave).algorithm(MacAlgorithm.HS256).build();
        final var token = assinar(outroEncoder, Instant.now().plusSeconds(1800));

        final var decoder = configuracao.jwtDecoder(chave);
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void deveRejeitarTokenExpirado() {
        final var configuracao = new ConfiguracaoSeguranca();
        final var chave = configuracao.secretKey(propriedades(new byte[TAMANHO_MINIMO_SEGREDO_BYTES]));
        final var token = assinar(configuracao.jwtEncoder(chave), Instant.now().minusSeconds(1));
        final var decoder = configuracao.jwtDecoder(chave);
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    private static String assinar(final JwtEncoder jwtEncoder, final Instant expiraEm) {
        final var claims = JwtClaimsSet.builder().subject("admin").expiresAt(expiraEm).build();
        final var parametros = JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims);
        return jwtEncoder.encode(parametros).getTokenValue();
    }
    private static PropriedadesSeguranca propriedades(final byte[] bytes) {
        final var segredo = Base64.getEncoder().encodeToString(bytes);
        return new PropriedadesSeguranca(
            new PropriedadesSeguranca.Admin("admin", "hash"),
            new PropriedadesSeguranca.Jwt(segredo, Duration.ofMinutes(30))
        );
    }
}
