package com.oficina.api.infraestrutura.persistencia.cliente;

import com.oficina.api.dominio.cliente.Documento;
import com.oficina.api.dominio.cliente.Documento.CNPJ;
import com.oficina.api.dominio.cliente.Documento.CPF;
import com.oficina.api.infraestrutura.persistencia.Criptografia;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class CriptografiaDocumento extends Criptografia<Documento> {
    public CriptografiaDocumento(
        @Value(value = "${app.clientes.chave-criptografia}") final String chaveCriptografia,
        @Value(value = "${app.clientes.chave-hash}") final String chaveHash
    ) {
        super(chaveCriptografia, chaveHash);
    }

    @Override
    protected Documento desserializar(final String texto) {
        if (texto.startsWith("CPF:")) {
            return new CPF(texto.substring(4));
        }
        if (texto.startsWith("CNPJ:")) {
            return new CNPJ(texto.substring(5));
        }
        throw new IllegalArgumentException("Tipo de documento criptografado inválido");
    }

    @Override
    protected String serializar(final Documento documento) {
        return documento.tipo().name() + ":" + documento.valor();
    }
}
