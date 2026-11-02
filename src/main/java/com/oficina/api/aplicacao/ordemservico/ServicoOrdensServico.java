package com.oficina.api.aplicacao.ordemservico;

import com.oficina.api.aplicacao.OperacoesTransacionais;
import com.oficina.api.aplicacao.Pagina;
import com.oficina.api.aplicacao.cliente.ClienteNaoEncontradoException;
import com.oficina.api.aplicacao.estoque.ServicoReservasInsumos;
import com.oficina.api.aplicacao.insumo.InsumoNaoEncontradoException;
import com.oficina.api.aplicacao.servico.ServicoNaoEncontradoException;
import com.oficina.api.aplicacao.veiculo.VeiculoNaoEncontradoException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

@Service
public class ServicoOrdensServico {
    private static final Logger LOG = LoggerFactory.getLogger(ServicoOrdensServico.class);
    private final ServicoReservasInsumos reservas;
    private final RepositorioOrdensServico repositorio;
    private final RepositorioClientes clientes;
    private final RepositorioVeiculos veiculos;
    private final RepositorioServicos servicos;
    private final RepositorioInsumos insumos;
    private final OperacoesTransacionais operacoes;
    private final Duration validadeAcesso;
    private final ApplicationEventPublisher eventos;

    public ServicoOrdensServico(
        final RepositorioOrdensServico repositorio,
        final ServicoReservasInsumos reservas,
        final RepositorioClientes clientes,
        final RepositorioVeiculos veiculos,
        final RepositorioServicos servicos,
        final RepositorioInsumos insumos,
        final ApplicationEventPublisher eventos,
        final ObservationRegistry observacoes,
        final PlatformTransactionManager transacoes,
        @Value(value = "${app.ordens-servico.validade-acesso:30d}") final Duration validadeAcesso
    ) {
        this.reservas = reservas;
        this.eventos = eventos;
        this.repositorio = repositorio;
        this.clientes = clientes;
        this.veiculos = veiculos;
        this.servicos = servicos;
        this.insumos = insumos;
        this.operacoes = new OperacoesTransacionais("ordens_servico.operacao", observacoes, transacoes);
        this.validadeAcesso = validadeAcesso;
    }

    public AcessoOrdemServico criar(final UUID veiculoId, final Collection<UUID> servicoIds, final String autor) {
        return this.operacoes.escrita("criar", () -> {
            final var veiculo = this.veiculos
                .buscar(veiculoId, LockModeType.PESSIMISTIC_READ)
                .orElseThrow(VeiculoNaoEncontradoException::new);
            final var clienteId = veiculo.clienteId();
            if (this.clientes.buscar(clienteId, LockModeType.PESSIMISTIC_READ).isEmpty()) {
                throw new ClienteNaoEncontradoException();
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
            notificar(ordem, token, false);
            if (ordem.status() == StatusOrdemServico.AGUARDANDO_CONFIRMACAO) {
                emitirDecisao(ordem);
            }
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
            this.reservas.bloquear();
            final var ordem = this.repositorio
                .buscar(id, LockModeType.PESSIMISTIC_WRITE)
                .orElseThrow(OrdemServicoNaoEncontradaException::new);
            final var anterior = ordem.status();
            acao.accept(ordem);
            if (anterior == StatusOrdemServico.AGUARDANDO_EXECUCAO
                && ordem.status() == StatusOrdemServico.EM_EXECUCAO) {
                this.reservas.consumir(ordem);
            }
            if (anterior != ordem.status() && ordem.status() == StatusOrdemServico.AGUARDANDO_CONFIRMACAO) {
                emitirDecisao(ordem);
            }
            if (anterior != ordem.status() && ordem.status() == StatusOrdemServico.ENTREGUE) {
                this.eventos
                    .publishEvent(
                        new EntregaOrdemServico(
                            Duration.between(ordem.criadaEm(), ordem.eventos().getLast().ocorridoEm())
                        )
                    );
            }
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
            this.reservas.bloquear();
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
        final var ordem = executar(id, "renovar_acesso", o -> {
            o.renovarAcesso(TokenAcompanhamento.hash(token), Instant.now().plus(this.validadeAcesso), autor);
            notificar(o, token, false);
            if (o.status() == StatusOrdemServico.AGUARDANDO_CONFIRMACAO) {
                emitirDecisao(o);
            }
        });
        return new AcessoOrdemServico(ordem, token);
    }

    public OrdemServico acompanhar(final String token) {
        return this.operacoes.leitura("acompanhar", () -> encontrarPorToken(token, LockModeType.NONE));
    }

    public OrdemServico decidir(final String token, final boolean aprovar) {
        return this.operacoes.escrita("decidir", () -> {
            this.reservas.bloquear();
            final var ordem = encontrarPorTokenDecisao(token, LockModeType.PESSIMISTIC_WRITE);
            if (aprovar) {
                ordem.aprovar();
                if (ordem.status() == StatusOrdemServico.AGUARDANDO_INSUMOS) {
                    this.reservas.reservar(ordem);
                }
            } else {
                ordem.rejeitar();
            }
            return ordem;
        });
    }

    public OrdemServico consultarDecisao(final String token) {
        return this.operacoes.leitura("consultar_decisao", () -> encontrarPorTokenDecisao(token, LockModeType.NONE));
    }

    private void emitirDecisao(final OrdemServico ordem) {
        final var token = TokenAcompanhamento.gerar();
        ordem.emitirAcessoDecisao(TokenAcompanhamento.hash(token), Instant.now().plus(this.validadeAcesso));
        notificar(ordem, token, true);
    }

    private void notificar(final OrdemServico ordem, final String token, final boolean decisao) {
        final var cliente = this.clientes.buscar(ordem.clienteId(), LockModeType.NONE);
        if (cliente.isEmpty()) {
            LOG.warn("Notificação da OS {} não emitida: cliente indisponível", ordem.id());
            return;
        }
        this.eventos.publishEvent(new EmailOrdemServico(ordem.id(), cliente.get().email().valor(), token, decisao));
    }

    private OrdemServico encontrarPorTokenDecisao(final String token, final LockModeType modo) {
        if (!token.matches("[A-Za-z0-9_-]{43}")) {
            throw new OrdemServicoNaoEncontradaException();
        }
        return this.repositorio
            .buscarPorTokenDecisao(TokenAcompanhamento.hash(token), modo)
            .orElseThrow(OrdemServicoNaoEncontradaException::new);
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
