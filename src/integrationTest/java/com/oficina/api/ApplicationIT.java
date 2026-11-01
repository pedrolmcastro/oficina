package com.oficina.api;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@Import(value = ConfiguracaoIntegration.class)
class ApplicationIT {
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void deveConectarAoSchema() {
        try (final var entityManager = this.entityManagerFactory.createEntityManager()) {
            assertThat(entityManager.createNativeQuery("SELECT current_schema()", String.class).getSingleResult())
                .isEqualTo("oficina");
        }
    }
}
