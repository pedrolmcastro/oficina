package com.oficina.api.infraestrutura.persistencia;

import com.oficina.api.dominio.cliente.Documento.CPF;
import com.oficina.api.infraestrutura.persistencia.cliente.CriptografiaDocumento;
import com.oficina.api.infraestrutura.persistencia.cliente.DocumentoPersistido;
import org.hibernate.metamodel.spi.ValueAccess;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TipoCriptografadoTest {
    private static final String CHAVE_AES = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String CHAVE_HMAC = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";
    private final CriptografiaDocumento criptografia = new CriptografiaDocumento(CHAVE_AES, CHAVE_HMAC);
    private final DocumentoPersistido tipo = new DocumentoPersistido(this.criptografia);

    @Test
    void deveIsolarArrays() {
        final var cifrado = new byte[] { 1, };
        final var hash = new byte[] { 2, };
        final var colunas = new TipoCriptografado.Colunas(cifrado, hash);
        cifrado[0] ^= 1;
        hash[0] ^= 1;
        colunas.cifrado()[0] ^= 1;
        colunas.hash()[0] ^= 1;
        assertThat(colunas.cifrado()).containsExactly((byte) 1);
        assertThat(colunas.hash()).containsExactly((byte) 2);
    }

    @Test
    void deveLerValorAusente() {
        assertThat(this.tipo.instantiate(mock(ValueAccess.class))).isNull();
    }

    @Test
    void deveRecuperarValorDoCacheProtegido() {
        final var documento = new CPF("52998224725");
        assertThat(this.tipo.assemble(this.tipo.disassemble(documento), null)).isEqualTo(documento);
    }

    @Test
    void deveRejeitarPropriedadeDesconhecida() {
        final var documento = new CPF("52998224725");
        assertThatThrownBy(() -> this.tipo.getPropertyValue(documento, 2))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Propriedade de valor protegido desconhecida");
    }

    @Test
    void deveProtegerCache() {
        assertThat(this.tipo.disassemble(null)).isNull();
        assertThat(this.tipo.assemble(null, null)).isNull();
        assertThatThrownBy(() -> this.tipo.assemble("inválido", null))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Representação de valor protegido no cache inválida");
    }

    @Test
    void deveRejeitarValorSemHash() {
        final var valores = mock(ValueAccess.class);
        when(valores.getValue(0, byte[].class)).thenReturn(this.criptografia.criptografar(new CPF("52998224725")));
        assertThatThrownBy(() -> this.tipo.instantiate(valores))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("Valor persistido sem HMAC");
    }

    @Test
    void deveRejeitarHashIncompativel() {
        final var cifrado = this.criptografia.criptografar(new CPF("52998224725"));
        final var outroHash = this.criptografia.calcularHash(new CPF("11144477735"));
        final var valores = valores(cifrado, outroHash);
        assertThatThrownBy(() -> this.tipo.instantiate(valores))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("HMAC incompatível com o valor persistido");
    }

    private static ValueAccess valores(final byte[] cifrado, final byte[] hash) {
        final var valores = mock(ValueAccess.class);
        when(valores.getValue(0, byte[].class)).thenReturn(cifrado);
        when(valores.getValue(1, byte[].class)).thenReturn(hash);
        return valores;
    }
}
