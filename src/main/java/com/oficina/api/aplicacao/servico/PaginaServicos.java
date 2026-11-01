package com.oficina.api.aplicacao.servico;

import com.oficina.api.dominio.servico.Servico;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record PaginaServicos(
    List<Servico> servicos,
    @Nullable
    UUID nextCursor
) {
    public PaginaServicos {
        servicos = List.copyOf(servicos);
    }
}
