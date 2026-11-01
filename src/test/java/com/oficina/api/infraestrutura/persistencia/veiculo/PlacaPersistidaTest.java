package com.oficina.api.infraestrutura.persistencia.veiculo;

import com.oficina.api.dominio.veiculo.Placa;
import org.hibernate.metamodel.spi.ValueAccess;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlacaPersistidaTest {
    private final ProtecaoPlaca protecao = new ProtecaoPlaca(
        "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA="
    );
    private final PlacaPersistida tipo = new PlacaPersistida(this.protecao);

    @Test
    void deveIsolarArrays() {
        final var cifrado = new byte[] { 1, };
        final var hash = new byte[] { 2, };
        final var colunas = new PlacaPersistida.Colunas(cifrado, hash);
        cifrado[0] ^= 1;
        hash[0] ^= 1;
        colunas.cifrado()[0] ^= 1;
        colunas.hash()[0] ^= 1;
        assertThat(colunas.cifrado()).containsExactly((byte) 1);
        assertThat(colunas.hash()).containsExactly((byte) 2);
    }

    @Test
    void deveLerPlacaAusente() {
        assertThat(this.tipo.instantiate(mock(ValueAccess.class))).isNull();
    }

    @Test
    void devePersistirPlaca() {
        final var placa = new Placa("ABC1234");
        final var cifrado = (byte[]) this.tipo.getPropertyValue(placa, 0);
        final var hash = (byte[]) this.tipo.getPropertyValue(placa, 1);
        assertThat(this.tipo.instantiate(valores(cifrado, hash))).isEqualTo(placa);
        assertThatThrownBy(() -> this.tipo.getPropertyValue(placa, 2))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Propriedade de placa desconhecida");
    }

    @Test
    void devePreservarContratoDeValorImutavel() {
        final var placa = new Placa("ABC1234");
        assertThat(this.tipo.equals(placa, new Placa("ABC1234"))).isTrue();
        assertThat(this.tipo.hashCode(placa)).isEqualTo(placa.hashCode());
        assertThat(this.tipo.replace(placa, new Placa("DEF1G23"), null)).isSameAs(placa);
    }

    @Test
    void deveProtegerCache() {
        final var placa = new Placa("ABC1234");
        final var cache = this.tipo.disassemble(placa);
        assertThat(this.tipo.assemble(cache, null)).isEqualTo(placa);
        assertThat(this.tipo.disassemble(null)).isNull();
        assertThat(this.tipo.assemble(null, null)).isNull();
        assertThatThrownBy(() -> this.tipo.assemble("inválido", null))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Representação de placa no cache inválida");
    }

    @Test
    void deveRejeitarPlacaSemHash() {
        final var valores = mock(ValueAccess.class);
        when(valores.getValue(0, byte[].class)).thenReturn(this.protecao.criptografar(new Placa("ABC1234")));
        assertThatThrownBy(() -> this.tipo.instantiate(valores))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("Placa persistida sem HMAC");
    }

    @Test
    void deveRejeitarHashIncompativel() {
        final var cifrado = this.protecao.criptografar(new Placa("ABC1234"));
        final var outroHash = this.protecao.calcularHash(new Placa("DEF1G23"));
        assertThatThrownBy(() -> this.tipo.instantiate(valores(cifrado, outroHash)))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("HMAC incompatível com a placa persistida");
    }

    private static ValueAccess valores(final byte[] cifrado, final byte[] hash) {
        final var valores = mock(ValueAccess.class);
        when(valores.getValue(0, byte[].class)).thenReturn(cifrado);
        when(valores.getValue(1, byte[].class)).thenReturn(hash);
        return valores;
    }
}
