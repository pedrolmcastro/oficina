package com.oficina.api.dominio.insumo;

import com.oficina.api.dominio.DadoInvalidoException;
import com.oficina.api.dominio.Preco;
import com.oficina.api.dominio.Quantidade;
import com.oficina.api.dominio.TextoNormalizado;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstoqueInsumoTest {
    @Test
    void devePreservarSaldosAoReservarEConsumir() {
        final var insumo = insumo();
        insumo.incrementarEstoque(new Quantidade(10));
        insumo.reservar(new Quantidade(7));
        insumo.consumirReserva(new Quantidade(4));
        assertThat(insumo.quantidadeEmEstoque()).isEqualTo(6);
        assertThat(insumo.quantidadeReservada()).isEqualTo(3);
        assertThat(insumo.quantidadeDisponivel()).isEqualTo(3);
    }

    @Test
    void deveRecusarReservasEBaixasMaioresQueOSaldo() {
        final var insumo = insumo();
        final var quantidade = new Quantidade(1);
        assertThatThrownBy(() -> insumo.reservar(quantidade)).isInstanceOf(DadoInvalidoException.class);
        assertThatThrownBy(() -> insumo.consumirReserva(quantidade)).isInstanceOf(DadoInvalidoException.class);
        assertThat(insumo.quantidadeEmEstoque()).isZero();
        assertThat(insumo.quantidadeReservada()).isZero();
    }

    @Test
    void deveRecusarOverflowSemAlterarSaldo() {
        final var insumo = insumo();
        insumo.incrementarEstoque(new Quantidade(Integer.MAX_VALUE));
        final var quantidade = new Quantidade(1);
        assertThatThrownBy(() -> insumo.incrementarEstoque(quantidade)).isInstanceOf(DadoInvalidoException.class);
        assertThat(insumo.quantidadeEmEstoque()).isEqualTo(Integer.MAX_VALUE);
    }

    private static Insumo insumo() {
        return new Insumo(new Nome(new TextoNormalizado("Óleo")), new PrecoVenda(new Preco(BigDecimal.TEN)));
    }
}
