package com.oficina.api.aplicacao.veiculo;

import com.oficina.api.dominio.veiculo.Veiculo;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record PaginaVeiculos(
    List<Veiculo> veiculos,
    @Nullable
    UUID nextCursor
) {
    public PaginaVeiculos {
        veiculos = List.copyOf(veiculos);
    }
}
