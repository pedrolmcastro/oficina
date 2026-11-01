package com.oficina.api;

import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import static com.oficina.api.infraestrutura.seguranca.ConfiguracaoSeguranca.TAMANHO_MINIMO_SEGREDO_BYTES;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@Import(value = ConfiguracaoIntegration.class)
class ApplicationIT {
    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void deveRejeitarAssinaturaDiferente() {
        final var outraChave = new SecretKeySpec(new byte[TAMANHO_MINIMO_SEGREDO_BYTES], "HmacSHA256");
        final var outroEncoder = NimbusJwtEncoder.withSecretKey(outraChave).algorithm(MacAlgorithm.HS256).build();
        final var token = assinar(outroEncoder, Instant.now().plusSeconds(1800));

        assertThatThrownBy(() -> this.jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void deveRejeitarTokenExpirado() {
        final var token = assinar(this.jwtEncoder, Instant.now().minusSeconds(1));
        assertThatThrownBy(() -> this.jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    private static String assinar(final JwtEncoder jwtEncoder, final Instant expiraEm) {
        final var claims = JwtClaimsSet.builder().subject("admin").expiresAt(expiraEm).build();
        final var parametros = JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims);
        return jwtEncoder.encode(parametros).getTokenValue();
    }
}
