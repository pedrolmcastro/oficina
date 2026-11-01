package com.oficina.api.aplicacao.servico;

import com.oficina.api.aplicacao.insumo.InsumoNaoEncontrado;
import com.oficina.api.dominio.insumo.Insumo;
import com.oficina.api.dominio.servico.Nome;
import com.oficina.api.dominio.servico.PrecoVenda;
import com.oficina.api.dominio.servico.Servico;
import com.oficina.api.infraestrutura.persistencia.insumo.RepositorioInsumos;
import com.oficina.api.infraestrutura.persistencia.servico.RepositorioServicos;
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
public class ServicoServicos {
    private final RepositorioServicos repositorio;
    private final RepositorioInsumos insumos;
    private final ObservationRegistry observacoes;
    private final TransactionTemplate leitura;
    private final TransactionTemplate escrita;

    public ServicoServicos(
        final RepositorioServicos repositorio,
        final RepositorioInsumos insumos,
        final ObservationRegistry observacoes,
        final PlatformTransactionManager transacoes
    ) {
        this.repositorio = repositorio;
        this.insumos = insumos;
        this.observacoes = observacoes;
        this.leitura = new TransactionTemplate(transacoes);
        this.leitura.setReadOnly(true);
        this.escrita = new TransactionTemplate(transacoes);
    }

    public Servico criar(final Nome nome, final PrecoVenda precoVenda, final boolean requerDiagnostico) {
        return observar("criar", this.escrita, () -> {
            final var servico = new Servico(nome, precoVenda, requerDiagnostico);
            this.repositorio.adicionar(servico);
            return servico;
        });
    }

    public Servico buscar(final UUID id) {
        return observar("buscar_id", this.leitura, () -> encontrar(id, LockModeType.NONE));
    }

    public PaginaServicos listar(final @Nullable String nome, final @Nullable UUID after, final int first) {
        if (first < 1 || first > 25) {
            throw new IllegalArgumentException("first deve estar entre 1 e 25");
        }
        return observar("listar", this.leitura, () -> {
            final var encontrados = this.repositorio.listar(nome, after, first + 1);
            if (encontrados.size() > first) {
                final var pagina = encontrados.subList(0, first);
                return new PaginaServicos(pagina, pagina.getLast().id());
            }
            return new PaginaServicos(encontrados, null);
        });
    }

    public Servico atualizar(
        final UUID id,
        final @Nullable Nome nome,
        final @Nullable PrecoVenda precoVenda,
        final @Nullable Boolean requerDiagnostico
    ) {
        return observar("atualizar", this.escrita, () -> {
            final var servico = encontrar(id, LockModeType.PESSIMISTIC_WRITE);
            if (nome != null) {
                servico.setNome(nome);
            }
            if (precoVenda != null) {
                servico.setPrecoVenda(precoVenda);
            }
            if (requerDiagnostico != null) {
                servico.setRequerDiagnostico(requerDiagnostico);
            }
            return servico;
        });
    }

    public void excluir(final UUID id) {
        observar("excluir", this.escrita, () -> {
            encontrar(id, LockModeType.PESSIMISTIC_WRITE).excluir();
            return Boolean.TRUE;
        });
    }

    public Insumo associarInsumoObrigatorio(final UUID servicoId, final UUID insumoId) {
        return observar("associar_insumo_obrigatorio", this.escrita, () -> {
            encontrar(servicoId, LockModeType.PESSIMISTIC_WRITE);
            final var insumo = this.insumos
                .buscar(insumoId, LockModeType.PESSIMISTIC_WRITE)
                .orElseThrow(InsumoNaoEncontrado::new);
            this.repositorio.associarInsumoObrigatorio(servicoId, insumoId);
            return insumo;
        });
    }

    public PaginaInsumosObrigatorios listarInsumosObrigatorios(
        final UUID servicoId,
        final @Nullable UUID after,
        final int first
    ) {
        if (first < 1 || first > 25) {
            throw new IllegalArgumentException("first deve estar entre 1 e 25");
        }
        return observar("listar_insumos_obrigatorios", this.leitura, () -> {
            encontrar(servicoId, LockModeType.NONE);
            final var encontrados = this.repositorio.listarInsumosObrigatorios(servicoId, after, first + 1);
            if (encontrados.size() > first) {
                final var pagina = encontrados.subList(0, first);
                return new PaginaInsumosObrigatorios(pagina, pagina.getLast().id());
            }
            return new PaginaInsumosObrigatorios(encontrados, null);
        });
    }

    public void removerInsumoObrigatorio(final UUID servicoId, final UUID insumoId) {
        observar("remover_insumo_obrigatorio", this.escrita, () -> {
            encontrar(servicoId, LockModeType.PESSIMISTIC_WRITE);
            if (!this.repositorio.removerInsumoObrigatorio(servicoId, insumoId)) {
                throw new InsumoObrigatorioNaoEncontrado();
            }
            return Boolean.TRUE;
        });
    }

    private Servico encontrar(final UUID id, final LockModeType modoBloqueio) {
        return this.repositorio.buscar(id, modoBloqueio).orElseThrow(ServicoNaoEncontrado::new);
    }

    private <T> T observar(final String operacao, final TransactionTemplate transacao, final Supplier<T> acao) {
        return Objects
            .requireNonNull(
                Observation
                    .createNotStarted("servicos.operacao", this.observacoes)
                    .lowCardinalityKeyValue("operacao", operacao)
                    .observe(() -> transacao.execute(_ -> acao.get()))
            );
    }
}
