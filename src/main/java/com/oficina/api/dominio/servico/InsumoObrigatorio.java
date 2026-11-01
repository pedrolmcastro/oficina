package com.oficina.api.dominio.servico;

import com.oficina.api.dominio.Quantidade;
import java.util.UUID;

public record InsumoObrigatorio(UUID insumoId, Quantidade quantidade) {}
