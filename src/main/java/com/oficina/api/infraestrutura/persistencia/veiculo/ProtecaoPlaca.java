package com.oficina.api.infraestrutura.persistencia.veiculo;

import com.oficina.api.dominio.veiculo.Placa;
import com.oficina.api.infraestrutura.persistencia.ProtecaoTexto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class ProtecaoPlaca {
    private final ProtecaoTexto protecao;

    public ProtecaoPlaca(
        @Value(value = "${app.veiculos.chave-criptografia}") final String chaveCriptografia,
        @Value(value = "${app.veiculos.chave-hash}") final String chaveHash
    ) {
        this.protecao = new ProtecaoTexto(chaveCriptografia, chaveHash);
    }

    public byte[] criptografar(final Placa placa) {
        return this.protecao.criptografar(placa.valor());
    }

    public Placa descriptografar(final byte[] envelope) {
        return new Placa(this.protecao.descriptografar(envelope));
    }

    public byte[] calcularHash(final Placa placa) {
        return this.protecao.calcularHash(placa.valor());
    }
}
