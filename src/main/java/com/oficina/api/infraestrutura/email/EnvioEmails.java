package com.oficina.api.infraestrutura.email;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EnvioEmails {
    private static final Logger LOG = LoggerFactory.getLogger(EnvioEmails.class);
    private final JavaMailSender remetente;
    private final String endereco;

    public EnvioEmails(
        final JavaMailSender remetente,
        @Value(value = "${app.notificacoes.remetente}") final String endereco
    ) {
        this.remetente = remetente;
        this.endereco = endereco;
    }

    public void enviar(final UUID ordemId, final String destinatario, final String corpo) {
        final var mensagem = new SimpleMailMessage();
        mensagem.setFrom(this.endereco);
        mensagem.setTo(destinatario);
        mensagem.setText(corpo);
        try {
            this.remetente.send(mensagem);
        } catch (final MailException erro) {
            // Registrar apenas tipo e stack trace, pois mensagem e causas SMTP podem conter
            // destinatário, corpo ou token.
            LOG.warn("Falha ao enviar notificação da OS {}", ordemId, semDadosSensiveis(erro));
        }
    }

    private static MailException semDadosSensiveis(final MailException erro) {
        final var seguro = new MailSendException(erro.getClass().getSimpleName());
        seguro.setStackTrace(erro.getStackTrace());
        return seguro;
    }
}
