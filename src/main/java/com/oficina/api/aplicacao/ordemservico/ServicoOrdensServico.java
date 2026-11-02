package com.oficina.api.aplicacao.ordemservico;

import com.oficina.api.aplicacao.OperacoesTransacionais;
import com.oficina.api.aplicacao.Pagina;
import com.oficina.api.aplicacao.cliente.ClienteNaoEncontradoException;
import com.oficina.api.aplicacao.insumo.InsumoNaoEncontradoException;
import com.oficina.api.aplicacao.servico.ServicoNaoEncontradoException;
import com.oficina.api.aplicacao.veiculo.VeiculoNaoEncontradoException;
import com.oficina.api.dominio.DadoInvalidoException;
import com.oficina.api.dominio.Quantidade;
import com.oficina.api.dominio.insumo.Insumo;
import com.oficina.api.dominio.ordemservico.InsumoNecessario;
import com.oficina.api.dominio.ordemservico.OrdemServico;
import com.oficina.api.dominio.ordemservico.ServicoContratado;
import com.oficina.api.dominio.ordemservico.StatusOrdemServico;
import com.oficina.api.dominio.servico.InsumoObrigatorio;
import com.oficina.api.infraestrutura.persistencia.cliente.RepositorioClientes;
import com.oficina.api.infraestrutura.persistencia.insumo.RepositorioInsumos;
import com.oficina.api.infraestrutura.persistencia.ordemservico.RepositorioOrdensServico;
import com.oficina.api.infraestrutura.persistencia.servico.RepositorioServicos;
import com.oficina.api.infraestrutura.persistencia.veiculo.RepositorioVeiculos;
import com.oficina.api.infraestrutura.seguranca.TokenAcompanhamento;
import io.micrometer.observation.ObservationRegistry;
import jakarta.persistence.LockModeType;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

@Service
public class ServicoOrdensServico {
    private final RepositorioOrdensServico repositorio;
    private final RepositorioClientes clientes;
    private final RepositorioVeiculos veiculos;
    private final RepositorioServicos servicos;
    private final RepositorioInsumos insumos;
    private final OperacoesTransacionais operacoes;
    private final Duration validadeAcesso;

    public ServicoOrdensServico(
        final RepositorioOrdensServico repositorio,
        final RepositorioClientes clientes,
        final RepositorioVeiculos veiculos,
        final RepositorioServicos servicos,
        final RepositorioInsumos insumos,
        final ObservationRegistry observacoes,
        final PlatformTransactionManager transacoes,
        @Value(value = "${app.ordens-servico.validade-acesso:30d}") final Duration validadeAcesso
    ) {
        this.repositorio = repositorio;
        this.clientes = clientes;
        this.veiculos = veiculos;
        this.servicos = servicos;
        this.insumos = insumos;
        this.operacoes = new OperacoesTransacionais("ordens_servico.operacao", observacoes, transacoes);
        this.validadeAcesso = validadeAcesso;
    }

    public AcessoOrdemServico criar(
        final UUID clienteId,
        final UUID veiculoId,
        final Collection<UUID> servicoIds,
        final String autor
    ) {
        return this.operacoes.escrita("criar", () -> {
            if (this.clientes.buscar(clienteId, LockModeType.PESSIMISTIC_READ).isEmpty()) {
                throw new ClienteNaoEncontradoException();
            }
            final var veiculo = this.veiculos
                .buscar(veiculoId, LockModeType.PESSIMISTIC_READ)
                .orElseThrow(VeiculoNaoEncontradoException::new);
            if (!veiculo.clienteId().equals(clienteId)) {
                throw new DadoInvalidoException("Veículo não pertence ao cliente informado");
            }
            final var contratados = servicoIds
                .stream()
                .sorted()
                .map(
                    id -> new ServicoContratado(
                        this.servicos
                            .buscar(id, LockModeType.PESSIMISTIC_READ)
                            .orElseThrow(ServicoNaoEncontradoException::new)
                    )
                )
                .toList();
            final var token = TokenAcompanhamento.gerar();
            final var ordem = OrdemServico
                .criar(
                    clienteId,
                    veiculoId,
                    contratados,
                    TokenAcompanhamento.hash(token),
                    Instant.now().plus(this.validadeAcesso),
                    autor
                );
            for (final var contratado : contratados) {
                UUID after = null;
                Pagina<InsumoObrigatorio> pagina;
                do {
                    pagina = Pagina
                        .de(
                            this.servicos
                                .listarInsumosObrigatorios(contratado.servicoId(), after, new Pagina.Limite(25)),
                            new Pagina.Limite(25),
                            i -> i.insumoId()
                        );
                    for (final var obrigatorio : pagina.itens()) {
                        ordem
                            .associarObrigatorio(
                                encontrarInsumo(obrigatorio.insumoId()),
                                contratado.id(),
                                obrigatorio.quantidade()
                            );
                    }
                    after = pagina.nextCursor();
                } while (after != null);
            }
            this.repositorio.adicionar(ordem);
            return new AcessoOrdemServico(ordem, token);
        });
    }

    public OrdemServico buscar(final UUID id) {
        return this.operacoes.leitura("buscar_id", () -> encontrar(id));
    }

    public Pagina<OrdemServico> listar(
        final @Nullable StatusOrdemServico status,
        final @Nullable UUID after,
        final Pagina.Limite limite
    ) {
        return this.operacoes.pagina("listar", limite, () -> this.repositorio.listar(status, after, limite));
    }

    public OrdemServico executar(final UUID id, final String operacao, final Consumer<OrdemServico> acao) {
        return this.operacoes.escrita(operacao, () -> {
            final var ordem = this.repositorio
                .buscar(id, LockModeType.PESSIMISTIC_WRITE)
                .orElseThrow(OrdemServicoNaoEncontradaException::new);
            acao.accept(ordem);
            return ordem;
        });
    }

    public InsumoNecessario adicionarInsumo(
        final UUID id,
        final UUID insumoId,
        final Quantidade quantidade,
        final String autor
    ) {
        return this.operacoes.escrita("adicionar_insumo", () -> {
            final var ordem = this.repositorio
                .buscar(id, LockModeType.PESSIMISTIC_WRITE)
                .orElseThrow(OrdemServicoNaoEncontradaException::new);
            return ordem.adicionarInsumo(encontrarInsumo(insumoId), quantidade, autor);
        });
    }

    public InsumoNecessario setQuantidade(
        final UUID id,
        final UUID itemId,
        final Quantidade quantidade,
        final String autor
    ) {
        return this.operacoes.escrita("alterar_quantidade", () -> {
            final var ordem = this.repositorio
                .buscar(id, LockModeType.PESSIMISTIC_WRITE)
                .orElseThrow(OrdemServicoNaoEncontradaException::new);
            return ordem.setQuantidade(itemId, quantidade, autor);
        });
    }

    public AcessoOrdemServico renovarAcesso(final UUID id, final String autor) {
        final var token = TokenAcompanhamento.gerar();
        final var ordem = executar(
            id,
            "renovar_acesso",
            o -> o.renovarAcesso(TokenAcompanhamento.hash(token), Instant.now().plus(this.validadeAcesso), autor)
        );
        return new AcessoOrdemServico(ordem, token);
    }

    public OrdemServico acompanhar(final String token) {
        return this.operacoes.leitura("acompanhar", () -> encontrarPorToken(token, LockModeType.NONE));
    }

    public OrdemServico decidir(final String token, final boolean aprovar) {
        return this.operacoes.escrita("decidir", () -> {
            final var ordem = encontrarPorToken(token, LockModeType.PESSIMISTIC_WRITE);
            if (aprovar) {
                ordem.aprovar();
            } else {
                ordem.rejeitar();
            }
            return ordem;
        });
    }

    private OrdemServico encontrar(final UUID id) {
        return this.repositorio.buscar(id, LockModeType.NONE).orElseThrow(OrdemServicoNaoEncontradaException::new);
    }

    private OrdemServico encontrarPorToken(final String token, final LockModeType modo) {
        if (!token.matches("[A-Za-z0-9_-]{43}")) {
            throw new OrdemServicoNaoEncontradaException();
        }
        return this.repositorio
            .buscarPorToken(TokenAcompanhamento.hash(token), modo)
            .orElseThrow(OrdemServicoNaoEncontradaException::new);
    }

    private Insumo encontrarInsumo(final UUID id) {
        return this.insumos.buscar(id, LockModeType.PESSIMISTIC_READ).orElseThrow(InsumoNaoEncontradoException::new);
    }
}
