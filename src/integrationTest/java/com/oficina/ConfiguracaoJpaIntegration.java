package com.oficina;

import com.oficina.api.dominio.Entidade;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;

@Configuration(proxyBeanMethods = false)
@AutoConfigurationPackage
public class ConfiguracaoJpaIntegration {
    @Bean
    PersistenceManagedTypes entidadesGerenciadas() {
        return PersistenceManagedTypes.of(EntidadeTeste.class.getName());
    }

    @Entity
    @Table(name = "entidades_teste")
    public static class EntidadeTeste extends Entidade {}
}
