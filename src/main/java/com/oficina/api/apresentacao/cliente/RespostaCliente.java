package com.oficina.api.apresentacao.cliente;

import com.oficina.api.dominio.cliente.Cliente;
import com.oficina.api.dominio.cliente.Documento;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record RespostaCliente(
    UUID id,
    String nome,
    String email,
    Documento.Tipo tipoDocumento,
    @Schema(description = "CPF ou CNPJ, apenas dígitos")
    String documento
) {
    public static RespostaCliente de(final Cliente cliente) {
        return new RespostaCliente(
            cliente.id(),
            cliente.nome().valor(),
            cliente.email().valor(),
            cliente.documento().tipo(),
            cliente.documento().valor()
        );
    }
}
