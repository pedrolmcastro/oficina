package com.oficina.api.infraestrutura.email;

import com.oficina.api.aplicacao.ordemservico.EmailOrdemServico;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EnvioEmailsTest {
    @Test
    void deveEnviarSomenteLinksSemAssunto() {
        final var smtp = mock(JavaMailSender.class);
        final var envio = new EnvioEmails(smtp, "oficina@example.test");
        final var id = UUID.randomUUID();
        final var notificacoes = new NotificacoesOrdensServico(envio, "http://localhost:8080/");
        notificacoes.enviar(new EmailOrdemServico(id, "cliente@example.test", "segredo", true));
        final var mensagem = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(smtp).send(mensagem.capture());
        assertThat(mensagem.getValue().getFrom()).isEqualTo("oficina@example.test");
        assertThat(mensagem.getValue().getTo()).containsExactly("cliente@example.test");
        assertThat(mensagem.getValue().getSubject()).isNull();
        assertThat(mensagem.getValue().getText())
            .isEqualTo(
                "http://localhost:8080/cliente/segredo/aprovacao\nhttp://localhost:8080/cliente/segredo/rejeicao"
            );
        assertThat(new EmailOrdemServico(id, "cliente@example.test", "segredo", true).toString())
            .doesNotContain("cliente@example.test", "segredo");
    }

    @Test
    void deveConterFalhaSmtpSemFalharOperacaoConfirmada() {
        final var smtp = mock(JavaMailSender.class);
        doThrow(new MailSendException("Falha SMTP")).when(smtp).send(any(SimpleMailMessage.class));
        final var envio = new EnvioEmails(smtp, "oficina@example.test");
        assertThatCode(() -> envio.enviar(UUID.randomUUID(), "cliente@example.test", "link"))
            .doesNotThrowAnyException();
    }
}
