package com.oficina.api.infraestrutura.email;

import com.oficina.api.aplicacao.ordemservico.EmailOrdemServico;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.testcontainers.containers.GenericContainer;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

class EnvioEmailsIT {
    @Test
    void deveEntregarEmailSmtpAoMailpit() throws Exception {
        try (
            final var mailpit = new GenericContainer<>("axllent/mailpit:v1.33.0").withExposedPorts(1025, 8025);
            final var http = HttpClient.newHttpClient()
        ) {
            mailpit.start();
            final var smtp = new JavaMailSenderImpl();
            smtp.setHost(mailpit.getHost());
            smtp.setPort(mailpit.getMappedPort(1025));
            final var envio = new EnvioEmails(smtp, "oficina@example.test");
            final var notificacoes = new NotificacoesOrdensServico(envio, "http://localhost:8080");
            notificacoes.enviar(new EmailOrdemServico(UUID.randomUUID(), "cliente@example.test", "token", false));
            final var base = "http://" + mailpit.getHost() + ":" + mailpit.getMappedPort(8025);
            final var lista = http
                .send(
                    HttpRequest.newBuilder(URI.create(base + "/api/v1/messages")).build(),
                    HttpResponse.BodyHandlers.ofString()
                );
            assertThat(lista.statusCode()).isEqualTo(200);
            final var json = JsonMapper.builder().build().readTree(lista.body());
            assertThat(json.path("total").asInt()).isEqualTo(1);
            final var id = json.path("messages").get(0).path("ID").asString();
            final var mensagem = http
                .send(
                    HttpRequest.newBuilder(URI.create(base + "/api/v1/message/" + id)).build(),
                    HttpResponse.BodyHandlers.ofString()
                );
            final var conteudo = JsonMapper.builder().build().readTree(mensagem.body());
            assertThat(conteudo.path("Text").asString().strip())
                .isEqualTo("http://localhost:8080/cliente/token/status");
            assertThat(conteudo.path("To").get(0).path("Address").asString()).isEqualTo("cliente@example.test");
            assertThat(conteudo.path("Subject").asString()).isEmpty();
        }
    }
}
