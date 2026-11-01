package com.oficina;

import com.oficina.api.dominio.Entidade;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;
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
    public static class EntidadeTeste extends Entidade {
        private @Nullable String titulo;
        private @Nullable String descricao;

        public EntidadeTeste() {}

        public EntidadeTeste(final @Nullable String titulo, final @Nullable String descricao) {
            this.titulo = titulo;
            this.descricao = descricao;
        }

        public @Nullable String titulo() {
            return this.titulo;
        }

        public @Nullable String descricao() {
            return this.descricao;
        }
    }
}
