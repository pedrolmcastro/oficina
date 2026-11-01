package com.oficina.api.infraestrutura.seguranca;

import jakarta.servlet.DispatcherType;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class ConfiguracaoSeguranca {
    public static final int TAMANHO_MINIMO_SEGREDO_BYTES = 32;

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) {
        final var perfis = new JwtGrantedAuthoritiesConverter();
        perfis.setAuthoritiesClaimName("roles");
        perfis.setAuthorityPrefix("ROLE_");

        final var conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(perfis);

        return http
            .csrf(config -> config.disable()) // NOSONAR: S4502 Só aceita Bearer explícito; cookies não autenticam.
            .formLogin(config -> config.disable())
            .httpBasic(config -> config.disable())
            .logout(config -> config.disable())
            .requestCache(config -> config.disable())
            .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(
                autorizacao -> autorizacao
                    .dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/login")
                    .permitAll()
                    .anyRequest()
                    .hasRole("ADMIN")
            )
            .oauth2ResourceServer(servidor -> servidor.jwt(jwt -> jwt.jwtAuthenticationConverter(conversor)))
            .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
        final UserDetailsService userDetailsService,
        final PasswordEncoder passwordEncoder
    ) {
        final var provedor = new DaoAuthenticationProvider(userDetailsService);
        provedor.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provedor);
    }

    @Bean
    public JwtEncoder jwtEncoder(final SecretKey segredo) {
        return NimbusJwtEncoder.withSecretKey(segredo).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtDecoder jwtDecoder(final SecretKey segredo) {
        final var decodificador = NimbusJwtDecoder.withSecretKey(segredo).macAlgorithm(MacAlgorithm.HS256).build();
        decodificador.setJwtValidator(new JwtTimestampValidator(Duration.ZERO));
        return decodificador;
    }

    @Bean
    public SecretKey secretKey(final PropriedadesSeguranca properties) {
        final var bytes = Base64.getDecoder().decode(properties.jwt().segredo());

        if (bytes.length < TAMANHO_MINIMO_SEGREDO_BYTES) {
            throw new IllegalArgumentException(
                "JWT_SEGREDO deve conter pelo menos " + TAMANHO_MINIMO_SEGREDO_BYTES
                    + " bytes aleatorios codificados em Base64"
            );
        }

        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(final PropriedadesSeguranca properties) {
        final var admin = User
            .withUsername(properties.admin().nomeUsuario())
            .password(properties.admin().hashSenha())
            .roles("ADMIN")
            .build();

        return new InMemoryUserDetailsManager(admin);
    }
}
