package com.oficina.api.aplicacao.servico;

import com.oficina.api.dominio.Preco;
import com.oficina.api.dominio.TextoNormalizado;
import com.oficina.api.dominio.servico.Nome;
import com.oficina.api.dominio.servico.PrecoVenda;
import com.oficina.api.dominio.servico.Servico;
import com.oficina.api.infraestrutura.persistencia.insumo.RepositorioInsumos;
import com.oficina.api.infraestrutura.persistencia.servico.RepositorioServicos;
import io.micrometer.observation.ObservationRegistry;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServicoServicosTest {
    private final RepositorioServicos repositorio = mock(RepositorioServicos.class);
    private final PlatformTransactionManager transacoes = mock(PlatformTransactionManager.class);
    private final ServicoServicos servico = new ServicoServicos(
        this.repositorio,
        mock(RepositorioInsumos.class),
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
        final var servico = new Servico(
            new Nome(new TextoNormalizado("Óleo")),
            new PrecoVenda(new Preco(new BigDecimal("19.90"))),
            true
        );
        when(this.repositorio.buscar(servico.id(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(Optional.of(servico));
        final var atualizado = this.servico
            .atualizar(
                servico.id(),
                nome == null
                    ? null
                    : new Nome(new TextoNormalizado(nome)),
                preco == null
                    ? null
                    : new PrecoVenda(new Preco(preco)),
                null
            );
        assertThat(atualizado.id()).isEqualTo(servico.id());
        assertThat(atualizado.requerDiagnostico()).isTrue();
        assertThat(atualizado.nome().texto().valor()).isEqualTo(nomeEsperado);
        assertThat(atualizado.precoVenda().preco().valor()).isEqualByComparingTo(precoEsperado);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void deveAtualizarExigenciaDeDiagnostico(final boolean requerDiagnostico) {
        final var existente = new Servico(
            new Nome(new TextoNormalizado("Troca de óleo")),
            new PrecoVenda(new Preco(BigDecimal.TEN)),
            !requerDiagnostico
        );
        when(this.repositorio.buscar(existente.id(), LockModeType.PESSIMISTIC_WRITE))
            .thenReturn(Optional.of(existente));
        final var atualizado = this.servico.atualizar(existente.id(), null, null, requerDiagnostico);
        assertThat(atualizado.requerDiagnostico()).isEqualTo(requerDiagnostico);
        assertThat(atualizado.nome().texto().valor()).isEqualTo("Troca de óleo");
        assertThat(atualizado.precoVenda().preco().valor()).isEqualByComparingTo(BigDecimal.TEN);
    }
}
