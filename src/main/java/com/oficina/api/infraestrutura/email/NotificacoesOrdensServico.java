package com.oficina.api.infraestrutura.email;

import com.oficina.api.aplicacao.ordemservico.EmailOrdemServico;
import com.oficina.api.aplicacao.ordemservico.VeiculoDisponivelRetirada;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificacoesOrdensServico {
    private final EnvioEmails envio;
    private final String urlBase;

    public NotificacoesOrdensServico(
        final EnvioEmails envio,
        @Value(value = "${app.notificacoes.url-base}") final String urlBase
    ) {
        this.envio = envio;
        this.urlBase = urlBase.replaceAll("/+$", "");
    }

    @TransactionalEventListener
    public void enviar(final VeiculoDisponivelRetirada evento) {
        this.envio.enviar(evento.ordemId(), evento.destinatario(), "Veículo disponível para retirada");
    }

    @TransactionalEventListener
    public void enviar(final EmailOrdemServico evento) {
        final var base = this.urlBase + "/cliente/" + evento.token();
        final var corpo = evento.decisao()
            ? base + "/aprovacao\n" + base + "/rejeicao"
            : base + "/status";
        this.envio.enviar(evento.ordemId(), evento.destinatario(), corpo);
    }
}
