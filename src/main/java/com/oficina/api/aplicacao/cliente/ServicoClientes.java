package com.oficina.api.aplicacao.cliente;

import com.oficina.api.dominio.cliente.Cliente;
import com.oficina.api.dominio.cliente.Documento;
import com.oficina.api.dominio.cliente.Email;
import com.oficina.api.dominio.cliente.Nome;
import com.oficina.api.infraestrutura.persistencia.cliente.RepositorioClientes;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.persistence.LockModeType;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ServicoClientes {
    private final RepositorioClientes repositorio;
    private final ObservationRegistry observacoes;
    private final TransactionTemplate leitura;
    private final TransactionTemplate escrita;

    public ServicoClientes(
        final RepositorioClientes repositorio,
        final ObservationRegistry observacoes,
        final PlatformTransactionManager transacoes
    ) {
        this.repositorio = repositorio;
        this.observacoes = observacoes;
        this.leitura = new TransactionTemplate(transacoes);
        this.leitura.setReadOnly(true);
        this.escrita = new TransactionTemplate(transacoes);
    }

    public Cliente criar(final Nome nome, final Email email, final Documento documento) {
        return observar("criar", this.escrita, () -> {
            final Cliente cliente = switch (documento) {
                case Documento.CPF cpf -> new Cliente.PessoaFisica(nome, email, cpf);
                case Documento.CNPJ cnpj -> new Cliente.PessoaJuridica(nome, email, cnpj);
            };
            this.repositorio.adicionar(cliente);
            return cliente;
        });
    }

    public Cliente buscar(final UUID id) {
        return observar("buscar_id", this.leitura, () -> encontrar(id, LockModeType.NONE));
    }

    public Cliente buscar(final Documento documento) {
        return observar(
            "buscar_documento",
            this.leitura,
            () -> this.repositorio.buscar(documento).orElseThrow(ClienteNaoEncontrado::new)
        );
    }

    public Cliente atualizar(
        final UUID id,
        final @Nullable Nome nome,
        final @Nullable Email email,
        final @Nullable Documento documento
    ) {
        return observar("atualizar", this.escrita, () -> {
            final var cliente = encontrar(id, LockModeType.PESSIMISTIC_WRITE);
            if (nome != null) {
                cliente.setNome(nome);
            }
            if (email != null) {
                cliente.setEmail(email);
            }
            if (documento != null) {
                cliente.setDocumento(documento);
            }
            return cliente;
        });
    }

    public void excluir(final UUID id) {
        observar("excluir", this.escrita, () -> {
            encontrar(id, LockModeType.PESSIMISTIC_WRITE).excluir();
            return Boolean.TRUE;
        });
    }

    private Cliente encontrar(final UUID id, final LockModeType modoBloqueio) {
        return this.repositorio.buscar(id, modoBloqueio).orElseThrow(ClienteNaoEncontrado::new);
    }

    private <T> T observar(final String operacao, final TransactionTemplate transacao, final Supplier<T> acao) {
        return Objects
            .requireNonNull(
                Observation
                    .createNotStarted("clientes.operacao", this.observacoes)
                    .lowCardinalityKeyValue("operacao", operacao)
                    .observe(() -> transacao.execute(_ -> acao.get()))
            );
    }
}
