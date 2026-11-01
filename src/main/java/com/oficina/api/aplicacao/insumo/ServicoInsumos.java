package com.oficina.api.aplicacao.insumo;

import com.oficina.api.dominio.insumo.Insumo;
import com.oficina.api.dominio.insumo.Nome;
import com.oficina.api.dominio.insumo.PrecoVenda;
import com.oficina.api.infraestrutura.persistencia.insumo.RepositorioInsumos;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.persistence.LockModeType;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ServicoInsumos {
    private final RepositorioInsumos repositorio;
    private final ObservationRegistry observacoes;
    private final TransactionTemplate leitura;
    private final TransactionTemplate escrita;

    public ServicoInsumos(
        final RepositorioInsumos repositorio,
        final ObservationRegistry observacoes,
        final PlatformTransactionManager transacoes
    ) {
        this.repositorio = repositorio;
        this.observacoes = observacoes;
        this.leitura = new TransactionTemplate(transacoes);
        this.leitura.setReadOnly(true);
        this.escrita = new TransactionTemplate(transacoes);
    }

    public Insumo criar(final Nome nome, final PrecoVenda precoVenda) {
        return observar("criar", this.escrita, () -> {
            final var insumo = new Insumo(nome, precoVenda);
            this.repositorio.adicionar(insumo);
            return insumo;
        });
    }

    public Insumo buscar(final UUID id) {
        return observar("buscar_id", this.leitura, () -> encontrar(id, LockModeType.NONE));
    }

    public PaginaInsumos listar(final @Nullable String nome, final @Nullable UUID after, final int first) {
        if (first < 1 || first > 25) {
            throw new IllegalArgumentException("first deve estar entre 1 e 25");
        }
        return observar("listar", this.leitura, () -> {
            final var encontrados = this.repositorio.listar(nome, after, first + 1);
            if (encontrados.size() > first) {
                final var pagina = encontrados.subList(0, first);
                return new PaginaInsumos(pagina, pagina.getLast().id());
            }
            return new PaginaInsumos(encontrados, null);
        });
    }

    public Insumo atualizar(final UUID id, final @Nullable Nome nome, final @Nullable PrecoVenda precoVenda) {
        return observar("atualizar", this.escrita, () -> {
            final var insumo = encontrar(id, LockModeType.PESSIMISTIC_WRITE);
            if (nome != null) {
                insumo.setNome(nome);
            }
            if (precoVenda != null) {
                insumo.setPrecoVenda(precoVenda);
            }
            return insumo;
        });
    }

    public void excluir(final UUID id) {
        observar("excluir", this.escrita, () -> {
            final var insumo = encontrar(id, LockModeType.PESSIMISTIC_WRITE);
            if (this.repositorio.obrigatorioParaServicoAtivo(id)) {
                throw new DataIntegrityViolationException("Insumo obrigatório de serviço ativo");
            }
            insumo.excluir();
            return Boolean.TRUE;
        });
    }

    private Insumo encontrar(final UUID id, final LockModeType modoBloqueio) {
        return this.repositorio.buscar(id, modoBloqueio).orElseThrow(InsumoNaoEncontrado::new);
    }

    private <T> T observar(final String operacao, final TransactionTemplate transacao, final Supplier<T> acao) {
        return Objects
            .requireNonNull(
                Observation
                    .createNotStarted("insumos.operacao", this.observacoes)
                    .lowCardinalityKeyValue("operacao", operacao)
                    .observe(() -> transacao.execute(_ -> acao.get()))
            );
    }
}
