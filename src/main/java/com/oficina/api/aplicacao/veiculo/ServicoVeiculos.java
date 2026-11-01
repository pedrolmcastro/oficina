package com.oficina.api.aplicacao.veiculo;

import com.oficina.api.aplicacao.cliente.ClienteNaoEncontrado;
import com.oficina.api.dominio.veiculo.Ano;
import com.oficina.api.dominio.veiculo.Marca;
import com.oficina.api.dominio.veiculo.Modelo;
import com.oficina.api.dominio.veiculo.Placa;
import com.oficina.api.dominio.veiculo.Veiculo;
import com.oficina.api.infraestrutura.persistencia.cliente.RepositorioClientes;
import com.oficina.api.infraestrutura.persistencia.veiculo.RepositorioVeiculos;
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
public class ServicoVeiculos {
    private final RepositorioVeiculos repositorio;
    private final RepositorioClientes clientes;
    private final ObservationRegistry observacoes;
    private final TransactionTemplate leitura;
    private final TransactionTemplate escrita;

    public ServicoVeiculos(
        final RepositorioVeiculos repositorio,
        final RepositorioClientes clientes,
        final ObservationRegistry observacoes,
        final PlatformTransactionManager transacoes
    ) {
        this.repositorio = repositorio;
        this.clientes = clientes;
        this.observacoes = observacoes;
        this.leitura = new TransactionTemplate(transacoes);
        this.leitura.setReadOnly(true);
        this.escrita = new TransactionTemplate(transacoes);
    }

    public Veiculo criar(
        final Placa placa,
        final Modelo modelo,
        final Marca marca,
        final Ano ano,
        final UUID clienteId
    ) {
        return observar("criar", this.escrita, () -> {
            validarCliente(clienteId);
            final var veiculo = new Veiculo(placa, modelo, marca, ano, clienteId);
            this.repositorio.adicionar(veiculo);
            return veiculo;
        });
    }

    public Veiculo buscar(final UUID id) {
        return observar("buscar_id", this.leitura, () -> encontrar(id, LockModeType.NONE));
    }

    public Veiculo buscar(final Placa placa) {
        return observar(
            "buscar_placa",
            this.leitura,
            () -> this.repositorio.buscar(placa).orElseThrow(VeiculoNaoEncontrado::new)
        );
    }

    public Veiculo atualizar(
        final UUID id,
        final @Nullable Modelo modelo,
        final @Nullable Marca marca,
        final @Nullable Placa placa,
        final @Nullable Ano ano,
        final @Nullable UUID clienteId
    ) {
        return observar("atualizar", this.escrita, () -> {
            final var veiculo = encontrar(id, LockModeType.PESSIMISTIC_WRITE);
            if (modelo != null) {
                veiculo.setModelo(modelo);
            }
            if (marca != null) {
                veiculo.setMarca(marca);
            }
            if (placa != null) {
                veiculo.setPlaca(placa);
            }
            if (ano != null) {
                veiculo.setAno(ano);
            }
            if (clienteId != null) {
                validarCliente(clienteId);
                veiculo.setClienteId(clienteId);
            }
            return veiculo;
        });
    }

    public void excluir(final UUID id) {
        observar("excluir", this.escrita, () -> {
            encontrar(id, LockModeType.PESSIMISTIC_WRITE).excluir();
            return Boolean.TRUE;
        });
    }

    private void validarCliente(final UUID clienteId) {
        if (this.clientes.buscar(clienteId, LockModeType.PESSIMISTIC_READ).isEmpty()) {
            throw new ClienteNaoEncontrado();
        }
    }

    private Veiculo encontrar(final UUID id, final LockModeType modoBloqueio) {
        return this.repositorio.buscar(id, modoBloqueio).orElseThrow(VeiculoNaoEncontrado::new);
    }

    private <T> T observar(final String operacao, final TransactionTemplate transacao, final Supplier<T> acao) {
        return Objects
            .requireNonNull(
                Observation
                    .createNotStarted("veiculos.operacao", this.observacoes)
                    .lowCardinalityKeyValue("operacao", operacao)
                    .observe(() -> transacao.execute(_ -> acao.get()))
            );
    }
}
