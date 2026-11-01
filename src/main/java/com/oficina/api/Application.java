package com.oficina.api;

import com.oficina.api.infraestrutura.seguranca.PropriedadesSeguranca;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "API Oficina", version = "v1", description = "API de gestão da oficina"))
@EnableConfigurationProperties(value = PropriedadesSeguranca.class)
public class Application {
    protected Application() {}

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
