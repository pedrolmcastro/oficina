package com.oficina.api.dominio;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EntidadeTest {
    @Test
    void deveDefinirIdentidade() {
        final var entidade = new Entidade() {};
        final var id = entidade.id();

        assertThat(id.variant()).isEqualTo(2);
        assertThat(id.version()).isEqualTo(7);

        assertThat(entidade)
            .isNotEqualTo((Entidade) new Entidade() {})
            .isNotEqualTo(new Object())
            .hasSameHashCodeAs(id);
    }
}
