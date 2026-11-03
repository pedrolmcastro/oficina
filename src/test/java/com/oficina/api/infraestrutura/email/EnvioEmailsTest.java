package com.oficina.api.infraestrutura.email;

import com.oficina.api.aplicacao.ordemservico.EmailOrdemServico;
import com.oficina.api.aplicacao.ordemservico.VeiculoDisponivelRetirada;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
    void deveNotificarVeiculoDisponivelParaRetirada() {
        final var smtp = mock(JavaMailSender.class);
        final var envio = new EnvioEmails(smtp, "oficina@example.test");
        final var evento = new VeiculoDisponivelRetirada(UUID.randomUUID(), "cliente@example.test");
        new NotificacoesOrdensServico(envio, "http://localhost:8080").enviar(evento);
        final var mensagem = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(smtp).send(mensagem.capture());
        assertThat(mensagem.getValue().getFrom()).isEqualTo("oficina@example.test");
        assertThat(mensagem.getValue().getTo()).containsExactly("cliente@example.test");
        assertThat(mensagem.getValue().getText()).isEqualTo("Veículo disponível para retirada");
        assertThat(evento.toString()).doesNotContain("cliente@example.test");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "http://localhost:8080,http://localhost:8080",
        "http://localhost:8080/,http://localhost:8080",
        "http://localhost:8080////,http://localhost:8080",
        "'',''",
        "/,''",
        "////,''" })
    void deveEnviarSomenteLinksSemAssunto(final String urlBase, final String baseEsperada) {
        final var smtp = mock(JavaMailSender.class);
        final var envio = new EnvioEmails(smtp, "oficina@example.test");
        final var id = UUID.randomUUID();
        final var notificacoes = new NotificacoesOrdensServico(envio, urlBase);
        notificacoes.enviar(new EmailOrdemServico(id, "cliente@example.test", "segredo", true));
        final var mensagem = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(smtp).send(mensagem.capture());
        assertThat(mensagem.getValue().getFrom()).isEqualTo("oficina@example.test");
        assertThat(mensagem.getValue().getTo()).containsExactly("cliente@example.test");
        assertThat(mensagem.getValue().getSubject()).isNull();
        assertThat(mensagem.getValue().getText())
            .isEqualTo(baseEsperada + "/cliente/segredo/aprovacao\n" + baseEsperada + "/cliente/segredo/rejeicao");
        assertThat(new EmailOrdemServico(id, "cliente@example.test", "segredo", true).toString())
            .doesNotContain("cliente@example.test", "segredo");
    }

    @Test
    @ExtendWith(value = OutputCaptureExtension.class)
    void deveConterFalhaSmtpSemFalharOperacaoConfirmada(final CapturedOutput saida) {
        final var smtp = mock(JavaMailSender.class);
        final var erro = new MailSendException(
            "Falha SMTP para cliente@example.test com link-secreto",
            new IllegalStateException("causa com dados privados")
        );
        doThrow(erro).when(smtp).send(any(SimpleMailMessage.class));
        final var envio = new EnvioEmails(smtp, "oficina@example.test");
        final var ordemId = UUID.randomUUID();
        assertThatCode(() -> envio.enviar(ordemId, "cliente@example.test", "link-secreto")).doesNotThrowAnyException();
        assertThat(saida.getOut())
            .contains(ordemId.toString(), "MailSendException", "EnvioEmails.enviar(EnvioEmails.java:")
            .doesNotContain("cliente@example.test", "link-secreto", "causa com dados privados");
    }
}
