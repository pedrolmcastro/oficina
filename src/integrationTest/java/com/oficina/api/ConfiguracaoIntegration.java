package com.oficina.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class ConfiguracaoIntegration {
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        final var container = new PostgreSQLContainer("postgres:18.6");
        container.withUrlParam("currentSchema", "oficina");
        return container;
    }

    @Bean
    DynamicPropertyRegistrar hashSenhaAdmin() {
        final var hashSenha = new BCryptPasswordEncoder().encode("admin");
        return registro -> registro.add("app.seguranca.admin.hash-senha", () -> hashSenha);
    }
}
