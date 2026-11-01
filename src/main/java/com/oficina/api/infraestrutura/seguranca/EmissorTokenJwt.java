package com.oficina.api.infraestrutura.seguranca;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public final class EmissorTokenJwt {
    public static record TokenEmitido(String valor, long validadeEmSegundos) {}

    private final JwtEncoder jwtEncoder;
    private final Duration validade;

    public EmissorTokenJwt(final JwtEncoder jwtEncoder, final PropriedadesSeguranca propriedades) {
        this.jwtEncoder = jwtEncoder;
        this.validade = propriedades.jwt().validade();

        if (!this.validade.isPositive()) {
            throw new IllegalArgumentException("A validade do JWT deve ser positiva");
        }
    }

    public TokenEmitido emitir(final String nomeUsuario, final Set<String> perfis) {
        final var agora = Instant.now();

        final var claims = JwtClaimsSet
            .builder()
            .subject(nomeUsuario)
            .issuedAt(agora)
            .expiresAt(agora.plus(validade))
            .claim("roles", List.copyOf(perfis))
            .build();

        final var cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        final var parametros = JwtEncoderParameters.from(cabecalho, claims);
        final var token = jwtEncoder.encode(parametros).getTokenValue();

        return new TokenEmitido(token, validade.toSeconds());
    }
}
