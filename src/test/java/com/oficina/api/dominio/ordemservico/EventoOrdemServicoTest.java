package com.oficina.api.dominio.ordemservico;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventoOrdemServicoTest {
    @Test
    void deveCompararEventosPelosValores() {
        final var instante = Instant.now();
        final var evento = new EventoOrdemServico(0, "CRIADA", instante, "admin", Map.of());
        assertThat(evento)
            .isEqualTo(new EventoOrdemServico(0, "CRIADA", instante, "admin", Map.of()))
            .isNotEqualTo(new EventoOrdemServico(1, "CRIADA", instante, "admin", Map.of()));
    }

    @Test
    void devePreservarDadosImutaveis() {
        final var dados = new HashMap<>(Map.of("orcamento", "100.00"));
        final var evento = new EventoOrdemServico(0, "ORCAMENTO_APROVADO", Instant.now(), "CLIENTE", dados);
        dados.put("orcamento", "200.00");
        final var dadosEvento = evento.dados();
        assertThat(dadosEvento).containsExactlyEntriesOf(Map.of("orcamento", "100.00"));
        assertThatThrownBy(() -> dadosEvento.put("orcamento", "300.00"))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
