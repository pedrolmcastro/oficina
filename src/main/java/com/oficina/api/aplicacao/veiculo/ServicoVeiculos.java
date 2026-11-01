package com.oficina.api.aplicacao.veiculo;

import com.oficina.api.aplicacao.OperacoesTransacionais;
import com.oficina.api.aplicacao.Pagina;
import com.oficina.api.aplicacao.cliente.ClienteNaoEncontradoException;
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
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

@Service
public class ServicoVeiculos {
    private final RepositorioVeiculos repositorio;
    private final RepositorioClientes clientes;
    private final OperacoesTransacionais operacoes;

    public ServicoVeiculos(
        final RepositorioVeiculos repositorio,
        final RepositorioClientes clientes,
        final ObservationRegistry observacoes,
        final PlatformTransactionManager transacoes
    ) {
        this.repositorio = repositorio;
        this.clientes = clientes;
        this.operacoes = new OperacoesTransacionais("veiculos.operacao", observacoes, transacoes);
    }

    public Veiculo criar(
        final Placa placa,
        final Modelo modelo,
        final Marca marca,
        final Ano ano,
        final UUID clienteId
    ) {
        return this.operacoes.escrita("criar", () -> {
            validarCliente(clienteId);
            final var veiculo = new Veiculo(placa, modelo, marca, ano, clienteId);
            this.repositorio.adicionar(veiculo);
            return veiculo;
        });
    }

    public Veiculo buscar(final UUID id) {
        return this.operacoes.leitura("buscar_id", () -> encontrar(id, LockModeType.NONE));
    }

    public Pagina<Veiculo> listar(
        final @Nullable Placa placa,
        final @Nullable UUID cliente,
        final @Nullable UUID after,
        final Pagina.Limite limite
    ) {
        if (placa == null && cliente == null) {
            throw new DadoInvalidoException("Informe ao menos um dos filtros: placa ou cliente");
        }
        return this.operacoes.pagina("listar", limite, () -> this.repositorio.listar(placa, cliente, after, limite));
    }

    public Veiculo atualizar(
        final UUID id,
        final @Nullable Modelo modelo,
        final @Nullable Marca marca,
        final @Nullable Placa placa,
        final @Nullable Ano ano,
        final @Nullable UUID clienteId
    ) {
        return this.operacoes.escrita("atualizar", () -> {
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
        this.operacoes.escrita("excluir", () -> encontrar(id, LockModeType.PESSIMISTIC_WRITE).excluir());
    }

    private void validarCliente(final UUID clienteId) {
        if (this.clientes.buscar(clienteId, LockModeType.PESSIMISTIC_READ).isEmpty()) {
            throw new ClienteNaoEncontradoException();
        }
    }

    private Veiculo encontrar(final UUID id, final LockModeType modoBloqueio) {
        return this.repositorio.buscar(id, modoBloqueio).orElseThrow(VeiculoNaoEncontradoException::new);
    }
}
