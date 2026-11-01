package com.oficina.api.infraestrutura.persistencia.cliente;

import com.oficina.api.dominio.cliente.Documento.CPF;
import org.hibernate.metamodel.spi.ValueAccess;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentoPersistidoTest {
    private final ProtecaoDocumento protecao = new ProtecaoDocumento(
        "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA="
    );
    private final DocumentoPersistido tipo = new DocumentoPersistido(this.protecao);

    @Test
    void deveIsolarArrays() {
        final var cifrado = new byte[] { 1, };
        final var hash = new byte[] { 2, };
        final var colunas = new DocumentoPersistido.Colunas(cifrado, hash);
        cifrado[0] ^= 1;
        hash[0] ^= 1;
        colunas.cifrado()[0] ^= 1;
        colunas.hash()[0] ^= 1;
        assertThat(colunas.cifrado()).containsExactly((byte) 1);
        assertThat(colunas.hash()).containsExactly((byte) 2);
    }

    @Test
    void deveLerDocumentoAusente() {
        assertThat(this.tipo.instantiate(mock(ValueAccess.class))).isNull();
    }

    @Test
    void devePersistirDocumento() {
        final var documento = new CPF("52998224725");
        final var cifrado = (byte[]) this.tipo.getPropertyValue(documento, 0);
        final var hash = (byte[]) this.tipo.getPropertyValue(documento, 1);
        assertThat(this.tipo.instantiate(valores(cifrado, hash))).isEqualTo(documento);
        assertThatThrownBy(() -> this.tipo.getPropertyValue(documento, 2))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Propriedade de documento desconhecida");
    }

    @Test
    void devePreservarContratoDeValorImutavel() {
        final var documento = new CPF("52998224725");
        assertThat(this.tipo.equals(documento, new CPF("52998224725"))).isTrue();
        assertThat(this.tipo.equals(documento, null)).isFalse();
        assertThat(this.tipo.equals(null, null)).isTrue();
        assertThat(this.tipo.hashCode(documento)).isEqualTo(documento.hashCode());
        assertThat(this.tipo.hashCode(null)).isZero();
        assertThat(this.tipo.replace(documento, new CPF("11144477735"), null)).isSameAs(documento);
        assertThat(this.tipo.replace(null, documento, null)).isNull();
    }

    @Test
    void deveProtegerCache() {
        final var documento = new CPF("52998224725");
        final var cache = this.tipo.disassemble(documento);
        assertThat(cache).isInstanceOf(DocumentoPersistido.Colunas.class);
        assertThat(this.tipo.assemble(cache, null)).isEqualTo(documento);
        assertThat(this.tipo.disassemble(null)).isNull();
        assertThat(this.tipo.assemble(null, null)).isNull();
        assertThatThrownBy(() -> this.tipo.assemble("inválido", null))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessage("Representação de documento no cache inválida");
    }

    @Test
    void deveRejeitarDocumentoSemHash() {
        final var valores = mock(ValueAccess.class);
        when(valores.getValue(0, byte[].class)).thenReturn(this.protecao.criptografar(new CPF("52998224725")));
        assertThatThrownBy(() -> this.tipo.instantiate(valores))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("Documento persistido sem HMAC");
    }

    @Test
    void deveRejeitarHashIncompativel() {
        final var cifrado = this.protecao.criptografar(new CPF("52998224725"));
        final var outroHash = this.protecao.calcularHash(new CPF("11144477735"));
        assertThatThrownBy(() -> this.tipo.instantiate(valores(cifrado, outroHash)))
            .isExactlyInstanceOf(IllegalStateException.class)
            .hasMessage("HMAC incompatível com o documento persistido");
    }

    private static ValueAccess valores(final byte[] cifrado, final byte[] hash) {
        final var valores = mock(ValueAccess.class);
        when(valores.getValue(0, byte[].class)).thenReturn(cifrado);
        when(valores.getValue(1, byte[].class)).thenReturn(hash);
        return valores;
    }
}
