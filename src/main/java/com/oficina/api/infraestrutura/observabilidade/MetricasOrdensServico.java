package com.oficina.api.infraestrutura.observabilidade;

import com.oficina.api.aplicacao.ordemservico.EntregaOrdemServico;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MetricasOrdensServico {
    private final Timer ciclo;

    public MetricasOrdensServico(final MeterRegistry metricas) {
        this.ciclo = metricas.timer("ordens_servico.ciclo");
    }

    @TransactionalEventListener
    public void registrar(final EntregaOrdemServico entrega) {
        this.ciclo.record(entrega.duracao());
    }
}
