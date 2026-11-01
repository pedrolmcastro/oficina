package com.oficina.api.aplicacao.servico;

import com.oficina.api.dominio.insumo.Insumo;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record PaginaInsumosObrigatorios(
    List<Insumo> insumosObrigatorios,
    @Nullable
    UUID nextCursor
) {
    public PaginaInsumosObrigatorios {
        insumosObrigatorios = List.copyOf(insumosObrigatorios);
    }
}
