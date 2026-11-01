package com.oficina.api.aplicacao.insumo;

import com.oficina.api.dominio.Preco;
import com.oficina.api.dominio.TextoNormalizado;
import com.oficina.api.dominio.insumo.Insumo;
import com.oficina.api.dominio.insumo.Nome;
import com.oficina.api.dominio.insumo.PrecoVenda;
import com.oficina.api.infraestrutura.persistencia.insumo.RepositorioInsumos;
import io.micrometer.observation.ObservationRegistry;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServicoInsumosTest {
    private final RepositorioInsumos repositorio = mock(RepositorioInsumos.class);
    private final PlatformTransactionManager transacoes = mock(PlatformTransactionManager.class);
    private final ServicoInsumos servico = new ServicoInsumos(
        this.repositorio,
        ObservationRegistry.NOOP,
        this.transacoes
    );

    @BeforeEach
    void configurarTransacoes() {
        when(this.transacoes.getTransaction(any())).thenAnswer(_ -> new SimpleTransactionStatus());
    }

    @ParameterizedTest
    @CsvSource(value = { "Filtro, , Filtro, 19.90", ", 25.15, Óleo, 25.15" })
    void deveAtualizarSomenteCamposInformados(
        final @Nullable String nome,
        final @Nullable BigDecimal preco,
        final String nomeEsperado,
        final BigDecimal precoEsperado
    ) {
        final var insumo = new Insumo(
            new Nome(new TextoNormalizado("Óleo")),
            new PrecoVenda(new Preco(new BigDecimal("19.90")))
        );
        when(this.repositorio.buscar(insumo.id(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(Optional.of(insumo));
        final var atualizado = this.servico
            .atualizar(
                insumo.id(),
                nome == null
                    ? null
                    : new Nome(new TextoNormalizado(nome)),
                preco == null
                    ? null
                    : new PrecoVenda(new Preco(preco))
            );
        assertThat(atualizado.id()).isEqualTo(insumo.id());
        assertThat(atualizado.nome().texto().valor()).isEqualTo(nomeEsperado);
        assertThat(atualizado.precoVenda().preco().valor()).isEqualByComparingTo(precoEsperado);
    }
}
