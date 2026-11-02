package com.oficina.api.aplicacao.ordemservico;

import com.oficina.api.dominio.ordemservico.OrdemServico;

public record AcessoOrdemServico(OrdemServico ordemServico, String token) {}
