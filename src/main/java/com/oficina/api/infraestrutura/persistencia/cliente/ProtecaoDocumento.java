package com.oficina.api.infraestrutura.persistencia.cliente;

import com.oficina.api.dominio.cliente.Documento;
import com.oficina.api.dominio.cliente.Documento.CNPJ;
import com.oficina.api.dominio.cliente.Documento.CPF;
import com.oficina.api.infraestrutura.persistencia.ProtecaoTexto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class ProtecaoDocumento {
    private final ProtecaoTexto protecao;

    public ProtecaoDocumento(
        @Value(value = "${app.clientes.chave-criptografia}") final String chaveCriptografia,
        @Value(value = "${app.clientes.chave-hash}") final String chaveHash
    ) {
        this.protecao = new ProtecaoTexto(chaveCriptografia, chaveHash);
    }

    public byte[] criptografar(final Documento documento) {
        return this.protecao.criptografar(serializar(documento));
    }

    public Documento descriptografar(final byte[] envelope) {
        final var texto = this.protecao.descriptografar(envelope);
        if (texto.startsWith("CPF:")) {
            return new CPF(texto.substring(4));
        }
        if (texto.startsWith("CNPJ:")) {
            return new CNPJ(texto.substring(5));
        }
        throw new IllegalArgumentException("Tipo de documento criptografado inválido");
    }

    public byte[] calcularHash(final Documento documento) {
        return this.protecao.calcularHash(serializar(documento));
    }

    private static String serializar(final Documento documento) {
        return documento.tipo().name() + ":" + documento.valor();
    }
}
