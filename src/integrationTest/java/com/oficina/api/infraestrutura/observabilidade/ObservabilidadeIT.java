package com.oficina.api.infraestrutura.observabilidade;

import com.oficina.api.ConfiguracaoIntegration;
import io.micrometer.core.instrument.MeterRegistry;
import io.restassured.RestAssured;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "management.server.port=0",
    "management.endpoints.web.exposure.include=prometheus" })
@Import(value = ConfiguracaoIntegration.class)
class ObservabilidadeIT {
    @Autowired
    private MeterRegistry registroMetricas;

    @LocalServerPort
    private int porta;

    @Value(value = "${local.management.port}")
    private int portaMetricas;

    @Test
    void deveExportarMetricas() {
        this.registroMetricas.timer("observabilidade.teste").record(Duration.ofMillis(125));

        final var metricas = RestAssured.given().port(this.portaMetricas).get("/actuator/prometheus");

        assertThat(metricas.statusCode()).isEqualTo(200);
        assertThat(metricas.asString().lines())
            .contains("observabilidade_teste_seconds_count 1", "observabilidade_teste_seconds_sum 0.125");
    }

    @Test
    void naoDeveUsarPortaPublica() {
        final var resposta = RestAssured.given().port(this.porta).get("/actuator/prometheus");

        assertThat(this.portaMetricas).isNotEqualTo(this.porta);
        assertThat(resposta.statusCode()).isEqualTo(401);
    }
}
