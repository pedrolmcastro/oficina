package com.oficina.api.aplicacao.cliente;

import com.oficina.api.aplicacao.OperacoesTransacionais;
import com.oficina.api.dominio.cliente.Cliente;
import com.oficina.api.dominio.cliente.Documento;
import com.oficina.api.dominio.cliente.Email;
import com.oficina.api.dominio.cliente.Nome;
import com.oficina.api.infraestrutura.persistencia.cliente.RepositorioClientes;
import io.micrometer.observation.ObservationRegistry;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

@Service
public class ServicoClientes {
    private final RepositorioClientes repositorio;
    private final OperacoesTransacionais operacoes;

    public ServicoClientes(
        final RepositorioClientes repositorio,
        final ObservationRegistry observacoes,
        final PlatformTransactionManager transacoes
    ) {
        this.repositorio = repositorio;
        this.operacoes = new OperacoesTransacionais("clientes.operacao", observacoes, transacoes);
    }

    public Cliente criar(final Nome nome, final Email email, final Documento documento) {
        return this.operacoes.escrita("criar", () -> {
            final var cliente = new Cliente(nome, email, documento);
            this.repositorio.adicionar(cliente);
            return cliente;
        });
    }

    public Cliente buscar(final UUID id) {
        return this.operacoes.leitura("buscar_id", () -> encontrar(id, LockModeType.NONE));
    }

    public Optional<Cliente> buscar(final Documento documento) {
        return this.operacoes.leitura("buscar_documento", () -> this.repositorio.buscar(documento));
    }

    public Cliente atualizar(
        final UUID id,
        final @Nullable Nome nome,
        final @Nullable Email email,
        final @Nullable Documento documento
    ) {
        return this.operacoes.escrita("atualizar", () -> {
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
        this.operacoes.escrita("excluir", () -> encontrar(id, LockModeType.PESSIMISTIC_WRITE).excluir());
    }

    private Cliente encontrar(final UUID id, final LockModeType modoBloqueio) {
        return this.repositorio.buscar(id, modoBloqueio).orElseThrow(ClienteNaoEncontradoException::new);
    }
}
