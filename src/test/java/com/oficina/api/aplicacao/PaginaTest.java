package com.oficina.api.aplicacao;

import com.oficina.api.dominio.DadoInvalidoException;
import com.oficina.api.dominio.Entidade;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginaTest {
    @ParameterizedTest
    @ValueSource(ints = { -1, 0, 26, Integer.MAX_VALUE })
    void deveRejeitarTamanhoInvalido(final int first) {
        assertThatThrownBy(() -> new Pagina.Limite(first)).isExactlyInstanceOf(DadoInvalidoException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 25 })
    void deveReservarRegistroAdicionalParaConsulta(final int valor) {
        final var limite = new Pagina.Limite(valor);
        assertThat(limite.valor()).isEqualTo(valor);
        assertThat(limite.quantidadeParaConsulta()).isEqualTo(valor + 1);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2 })
    void deveEncerrarPaginaSemRegistroExcedente(final int quantidade) {
        final var encontrados = IntStream.range(0, quantidade).mapToObj(_ -> entidade()).toList();
        final var pagina = Pagina.de(encontrados, new Pagina.Limite(2));
        assertThat(pagina.itens()).containsExactlyElementsOf(encontrados);
        assertThat(pagina.nextCursor()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 25 })
    void deveUsarUltimoItemDaPaginaComoCursorSemIncluirExcedente(final int first) {
        final var encontrados = IntStream.range(0, first + 1).mapToObj(_ -> entidade()).toList();
        final var pagina = Pagina.de(encontrados, new Pagina.Limite(first));
        assertThat(pagina.itens()).containsExactlyElementsOf(encontrados.subList(0, first));
        assertThat(pagina.nextCursor()).isEqualTo(encontrados.get(first - 1).id());
    }

    @Test
    void deveIsolarItensDeAlteracoesExternas() {
        final var entidade = entidade();
        final var encontrados = new ArrayList<>(List.of(entidade));
        final var pagina = Pagina.de(encontrados, new Pagina.Limite(1));
        encontrados.clear();
        assertThat(pagina.itens()).containsExactly(entidade);
        assertThatThrownBy(() -> pagina.itens().clear()).isExactlyInstanceOf(UnsupportedOperationException.class);
    }

    private static Entidade entidade() {
        return new Entidade() {};
    }
}
