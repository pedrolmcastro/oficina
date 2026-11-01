package com.oficina.api.aplicacao.cliente;

import com.oficina.api.dominio.TextoNormalizado;
import com.oficina.api.dominio.cliente.Cliente;
import com.oficina.api.dominio.cliente.Documento;
import com.oficina.api.dominio.cliente.Documento.CPF;
import com.oficina.api.dominio.cliente.Email;
import com.oficina.api.dominio.cliente.Nome;
import com.oficina.api.infraestrutura.persistencia.cliente.RepositorioClientes;
import io.micrometer.observation.ObservationRegistry;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServicoClientesTest {
    private final RepositorioClientes repositorio = mock(RepositorioClientes.class);
    private final PlatformTransactionManager transacoes = mock(PlatformTransactionManager.class);
    private final ServicoClientes servico = new ServicoClientes(
        this.repositorio,
        ObservationRegistry.NOOP,
        this.transacoes
    );

    @BeforeEach
    void configurarTransacoes() {
        when(this.transacoes.getTransaction(any())).thenAnswer(_ -> new SimpleTransactionStatus());
    }

    @ParameterizedTest
    @CsvSource(value = {
        "Novo nome, , , Novo nome, maria@example.com, 52998224725",
        ", novo@example.com, , Maria, novo@example.com, 52998224725",
        ", , 11144477735, Maria, maria@example.com, 11144477735" })
    void deveAtualizarSomenteCamposInformados(
        final @Nullable String nome,
        final @Nullable Email email,
        final @Nullable CPF documento,
        final String nomeEsperado,
        final String emailEsperado,
        final String documentoEsperado
    ) {
        final var cliente = new Cliente(
            new Nome(new TextoNormalizado("Maria")),
            new Email("maria@example.com"),
            Documento.de("52998224725")
        );
        when(this.repositorio.buscar(cliente.id(), LockModeType.PESSIMISTIC_WRITE)).thenReturn(Optional.of(cliente));

        final var atualizado = this.servico
            .atualizar(
                cliente.id(),
                nome == null
                    ? null
                    : new Nome(new TextoNormalizado(nome)),
                email,
                documento
            );

        assertThat(atualizado.id()).isEqualTo(cliente.id());
        assertThat(atualizado.nome().texto().valor()).isEqualTo(nomeEsperado);
        assertThat(atualizado.email().valor()).isEqualTo(emailEsperado);
        assertThat(atualizado.documento().valor()).isEqualTo(documentoEsperado);
    }

    @Test
    void deveRejeitarIdInexistente() {
        final var id = UUID.randomUUID();
        assertThatThrownBy(() -> this.servico.buscar(id)).isExactlyInstanceOf(ClienteNaoEncontradoException.class);
        final var nome = new Nome(new TextoNormalizado("Novo nome"));
        assertThatThrownBy(() -> this.servico.atualizar(id, nome, null, null))
            .isExactlyInstanceOf(ClienteNaoEncontradoException.class);
        assertThatThrownBy(() -> this.servico.excluir(id)).isExactlyInstanceOf(ClienteNaoEncontradoException.class);
    }

    @Test
    void deveRetornarVazioParaDocumentoInexistente() {
        final var documento = Documento.de("52998224725");
        assertThat(this.servico.buscar(documento)).isEmpty();
    }
}
