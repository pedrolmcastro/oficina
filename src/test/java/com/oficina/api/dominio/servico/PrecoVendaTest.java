package com.oficina.api.dominio.servico;

import com.oficina.api.dominio.DadoInvalidoException;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrecoVendaTest {
    @ParameterizedTest
    @ValueSource(strings = { "0", "0.01", "19.90", "19.900", "10000.00" })
    void devePreservarPrecoExatoEmCentavos(final String valor) {
        final var preco = new PrecoVenda(new BigDecimal(valor));
        assertThat(preco.valor()).isEqualByComparingTo(valor);
    }

    @ParameterizedTest
    @ValueSource(strings = { "-0.01", "0.001", "10000.01", "1E+100" })
    void deveRejeitarPrecoForaDoIntervaloOuComFracaoDeCentavo(final String valor) {
        assertThatThrownBy(() -> new PrecoVenda(new BigDecimal(valor))).isInstanceOf(DadoInvalidoException.class);
    }
}
