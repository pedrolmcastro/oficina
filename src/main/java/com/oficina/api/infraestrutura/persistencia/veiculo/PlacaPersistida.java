package com.oficina.api.infraestrutura.persistencia.veiculo;

import com.oficina.api.dominio.veiculo.Placa;
import com.oficina.api.infraestrutura.persistencia.TipoCriptografado;
import org.springframework.stereotype.Component;

@Component
public final class PlacaPersistida extends TipoCriptografado<Placa> {
    public PlacaPersistida(final CriptografiaPlaca criptografia) {
        super(criptografia, Placa.class);
    }
}
