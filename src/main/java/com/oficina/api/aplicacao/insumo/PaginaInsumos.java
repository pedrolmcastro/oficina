package com.oficina.api.aplicacao.insumo;

import com.oficina.api.dominio.insumo.Insumo;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record PaginaInsumos(
    List<Insumo> insumos,
    @Nullable
    UUID nextCursor
) {
    public PaginaInsumos {
        insumos = List.copyOf(insumos);
    }
}
