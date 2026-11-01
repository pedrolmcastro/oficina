package com.oficina.api;

import com.oficina.api.dominio.Entidade;
import com.oficina.suporte.ConfiguracaoEntidadeTeste;
import com.oficina.suporte.ConfiguracaoEntidadeTeste.EntidadeTeste;
import jakarta.persistence.EntityManager;
import java.util.HashSet;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.default_schema=public" })
@ContextConfiguration(classes = ConfiguracaoEntidadeTeste.class)
@Import(value = ConfiguracaoIntegration.class)
class EntidadeIT {
    @Autowired
    private EntityManager entityManager;

    @Test
    void devePreservarIgualdade() {
        final var entidade = new EntidadeTeste();
        final var entidades = new HashSet<Entidade>();
        entidades.add(entidade);
        this.entityManager.persist(entidade);
        this.entityManager.flush();
        this.entityManager.clear();

        final var recarregada = this.entityManager.find(EntidadeTeste.class, entidade.id());
        assertThat(recarregada).isNotSameAs(entidade).isEqualTo(entidade);
        assertThat(entidade).isEqualTo(recarregada);
        assertThat(recarregada).hasSameHashCodeAs(entidade);
        assertThat(entidades).contains(recarregada);
        this.entityManager.clear();

        final var referencia = this.entityManager.getReference(EntidadeTeste.class, entidade.id());
        assertThat(Hibernate.isInitialized(referencia)).isFalse();
        assertThat(entidade).isEqualTo(referencia);
        assertThat(referencia).isEqualTo(entidade).hasSameHashCodeAs(entidade);
        assertThat(entidades).contains(referencia);
    }
}
