package com.oficina.api.apresentacao.cliente;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record RespostaListaClientes(
    List<RespostaCliente> clientes,
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    @Schema(description = "Cursor para a próxima página; null na consulta por documento.", types = {
        "string",
        "null" }, format = "uuid")
    @Nullable
    UUID nextCursor
) {
    public RespostaListaClientes {
        clientes = List.copyOf(clientes);
    }
}
