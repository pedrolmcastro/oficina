package com.oficina.api.infraestrutura.persistencia;

import com.oficina.api.dominio.veiculo.Placa;
import com.oficina.api.infraestrutura.persistencia.veiculo.CriptografiaPlaca;
import com.oficina.api.infraestrutura.persistencia.veiculo.PlacaPersistida;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TipoImutavelTest {
    private final TipoImutavel<Placa> tipo = new PlacaPersistida(mock(CriptografiaPlaca.class));

    @Test
    void deveUsarIgualdadePorValorInclusiveParaAusentes() {
        final var placa = new Placa("ABC1234");
        assertThat(this.tipo.equals(placa, new Placa("ABC1234"))).isTrue();
        assertThat(this.tipo.equals(placa, new Placa("DEF1G23"))).isFalse();
        assertThat(this.tipo.equals(placa, null)).isFalse();
        assertThat(this.tipo.equals(null, null)).isTrue();
        assertThat(this.tipo.hashCode(placa)).isEqualTo(placa.hashCode());
        assertThat(this.tipo.hashCode(null)).isZero();
    }

    @Test
    void devePreservarReferenciaDoValorImutavel() {
        final var placa = new Placa("ABC1234");
        assertThat(this.tipo.isMutable()).isFalse();
        assertThat(this.tipo.deepCopy(placa)).isSameAs(placa);
        assertThat(this.tipo.replace(placa, new Placa("DEF1G23"), null)).isSameAs(placa);
        assertThat(this.tipo.deepCopy(null)).isNull();
        assertThat(this.tipo.replace(null, placa, null)).isNull();
    }
}
