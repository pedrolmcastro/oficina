package com.oficina.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistrar;

@TestConfiguration(proxyBeanMethods = false)
class ConfiguracaoBdd {
    @Bean
    DynamicPropertyRegistrar hashSenhaAdmin(final PasswordEncoder encoder) {
        final var hash = encoder.encode("admin");
        return registro -> registro.add("app.seguranca.admin.hash-senha", () -> hash);
    }
}
