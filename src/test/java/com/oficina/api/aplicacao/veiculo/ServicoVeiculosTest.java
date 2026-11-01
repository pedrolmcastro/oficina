package com.oficina.api.aplicacao.veiculo;

import com.oficina.api.dominio.DadoInvalidoException;
import com.oficina.api.dominio.veiculo.Ano;
import com.oficina.api.dominio.veiculo.Marca;
import com.oficina.api.dominio.veiculo.Modelo;
import com.oficina.api.dominio.veiculo.Placa;
import com.oficina.api.dominio.veiculo.Veiculo;
import com.oficina.api.infraestrutura.persistencia.cliente.RepositorioClientes;
import com.oficina.api.infraestrutura.persistencia.veiculo.RepositorioVeiculos;
import io.micrometer.observation.ObservationRegistry;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServicoVeiculosTest {
    private final RepositorioVeiculos repositorio = mock(RepositorioVeiculos.class);
    private final RepositorioClientes clientes = mock(RepositorioClientes.class);
    private final PlatformTransactionManager transacoes = mock(PlatformTransactionManager.class);
    private final ServicoVeiculos servico = new ServicoVeiculos(
        this.repositorio,
        this.clientes,
        ObservationRegistry.NOOP,
        this.transacoes
    );

    @BeforeEach
    void configurarTransacoes() {
        when(this.transacoes.getTransaction(any())).thenAnswer(_ -> new SimpleTransactionStatus());
    }

    @ParameterizedTest
    @CsvSource(value = {
        "Civic, , , , Civic, Toyota, ABC1234, 2024",
        ", Honda, , , Corolla, Honda, ABC1234, 2024",
        ", , DEF1G23, , Corolla, Toyota, DEF1G23, 2024",
        ", , , 2025, Corolla, Toyota, ABC1234, 2025" })
    void deveAtualizarSomenteCamposInformados(
        final @Nullable Modelo modelo,
        final @Nullable Marca marca,
        final @Nullable Placa placa,
        final @Nullable Integer ano,
        final String modeloEsperado,
        final String marcaEsperada,
        final String placaEsperada,
        final int anoEsperado
    ) {
        final var clienteId = UUID.randomUUID();
        final var veiculo = new Veiculo(
            new Placa("ABC1234"),
            new Modelo("Corolla"),
            new Marca("Toyota"),
            new Ano(2024),
            clienteId
        );
        when(this.repositorio.buscar(veiculo.id(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(Optional.of(veiculo));

        final var atualizado = this.servico
            .atualizar(
                veiculo.id(),
                modelo,
                marca,
                placa,
                ano == null
                    ? null
                    : new Ano(ano),
                null
            );

        assertThat(atualizado.id()).isEqualTo(veiculo.id());
        assertThat(atualizado.modelo().valor()).isEqualTo(modeloEsperado);
        assertThat(atualizado.marca().valor()).isEqualTo(marcaEsperada);
        assertThat(atualizado.placa().valor()).isEqualTo(placaEsperada);
        assertThat(atualizado.ano().valor()).isEqualTo(anoEsperado);
        assertThat(atualizado.clienteId()).isEqualTo(clienteId);
    }

    @Test
    void deveRejeitarIdInexistente() {
        final var id = UUID.randomUUID();
        assertThatThrownBy(() -> this.servico.buscar(id)).isExactlyInstanceOf(VeiculoNaoEncontrado.class);
        assertThatThrownBy(() -> this.servico.atualizar(id, new Modelo("Civic"), null, null, null, null))
            .isExactlyInstanceOf(VeiculoNaoEncontrado.class);
        assertThatThrownBy(() -> this.servico.excluir(id)).isExactlyInstanceOf(VeiculoNaoEncontrado.class);
    }

    @Test
    void deveExigirPlacaOuCliente() {
        assertThatThrownBy(() -> this.servico.listar(null, null, UUID.randomUUID(), 1))
            .isExactlyInstanceOf(DadoInvalidoException.class)
            .hasMessage("Informe ao menos um dos filtros: placa ou cliente");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 26 })
    void deveRejeitarTamanhoDePaginaInvalido(final int first) {
        assertThatThrownBy(() -> this.servico.listar(null, UUID.randomUUID(), null, first))
            .isExactlyInstanceOf(IllegalArgumentException.class);
    }
}
