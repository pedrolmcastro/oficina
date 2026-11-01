package com.oficina.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistrar;

@TestConfiguration(proxyBeanMethods = false)
class ConfiguracaoIntegration {
    @Bean
    DynamicPropertyRegistrar hashSenhaAdmin() {
        final var hashSenha = new BCryptPasswordEncoder().encode("admin");
        return registro -> registro.add("app.seguranca.admin.hash-senha", () -> hashSenha);
    }
}
