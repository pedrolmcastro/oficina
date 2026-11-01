package com.oficina.api.infraestrutura.persistencia.veiculo;

import com.oficina.api.dominio.veiculo.Placa;
import com.oficina.api.infraestrutura.persistencia.Criptografia;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class CriptografiaPlaca extends Criptografia<Placa> {
    public CriptografiaPlaca(
        @Value(value = "${app.veiculos.chave-criptografia}") final String chaveCriptografia,
        @Value(value = "${app.veiculos.chave-hash}") final String chaveHash
    ) {
        super(chaveCriptografia, chaveHash);
    }

    @Override
    protected String serializar(final Placa placa) {
        return placa.valor();
    }

    @Override
    protected Placa desserializar(final String texto) {
        return new Placa(texto);
    }
}
