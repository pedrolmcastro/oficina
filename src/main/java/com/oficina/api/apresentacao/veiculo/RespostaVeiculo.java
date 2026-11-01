package com.oficina.api.apresentacao.veiculo;

import com.oficina.api.dominio.veiculo.Veiculo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record RespostaVeiculo(
    UUID id,
    @Schema(description = "Placa em maiúsculas, sem hífen")
    String placa,
    String modelo,
    String marca,
    int ano,
    UUID clienteId
) {
    public static RespostaVeiculo de(final Veiculo veiculo) {
        return new RespostaVeiculo(
            veiculo.id(),
            veiculo.placa().valor(),
            veiculo.modelo().valor(),
            veiculo.marca().valor(),
            veiculo.ano().valor(),
            veiculo.clienteId()
        );
    }
}
