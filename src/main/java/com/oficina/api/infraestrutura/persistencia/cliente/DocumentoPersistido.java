package com.oficina.api.infraestrutura.persistencia.cliente;

import com.oficina.api.dominio.cliente.Documento;
import com.oficina.api.infraestrutura.persistencia.TipoCriptografado;
import org.springframework.stereotype.Component;

@Component
public final class DocumentoPersistido extends TipoCriptografado<Documento> {
    public DocumentoPersistido(final CriptografiaDocumento criptografia) {
        super(criptografia, Documento.class);
    }
}
