package com.oficina.api;

import com.oficina.api.infraestrutura.seguranca.PropriedadesSeguranca;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(value = PropriedadesSeguranca.class)
public class Application {
    protected Application() {}

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
