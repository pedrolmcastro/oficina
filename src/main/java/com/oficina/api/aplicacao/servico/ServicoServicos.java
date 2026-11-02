package com.oficina.api.aplicacao.servico;

import com.oficina.api.aplicacao.OperacoesTransacionais;
import com.oficina.api.aplicacao.Pagina;
import com.oficina.api.aplicacao.insumo.InsumoNaoEncontradoException;
import com.oficina.api.dominio.Quantidade;
import com.oficina.api.dominio.servico.InsumoObrigatorio;
import com.oficina.api.dominio.servico.Nome;
import com.oficina.api.dominio.servico.PrecoVenda;
import com.oficina.api.dominio.servico.Servico;
import com.oficina.api.infraestrutura.persistencia.insumo.RepositorioInsumos;
import com.oficina.api.infraestrutura.persistencia.servico.RepositorioServicos;
import io.micrometer.observation.ObservationRegistry;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

@Service
public class ServicoServicos {
    private final RepositorioServicos repositorio;
    private final RepositorioInsumos insumos;
    private final OperacoesTransacionais operacoes;

    public ServicoServicos(
        final RepositorioServicos repositorio,
        final RepositorioInsumos insumos,
        final ObservationRegistry observacoes,
        final PlatformTransactionManager transacoes
    ) {
        this.repositorio = repositorio;
        this.insumos = insumos;
        this.operacoes = new OperacoesTransacionais("servicos.operacao", observacoes, transacoes);
    }

    public Servico criar(final Nome nome, final PrecoVenda precoVenda, final boolean requerDiagnostico) {
        return this.operacoes.escrita("criar", () -> {
            final var servico = new Servico(nome, precoVenda, requerDiagnostico);
            this.repositorio.adicionar(servico);
            return servico;
        });
    }

    public Servico buscar(final UUID id) {
        return this.operacoes.leitura("buscar_id", () -> encontrar(id, LockModeType.NONE));
    }

    public Pagina<Servico> listar(final @Nullable String nome, final @Nullable UUID after, final Pagina.Limite limite) {
        return this.operacoes.pagina("listar", limite, () -> this.repositorio.listar(nome, after, limite));
    }

    public Servico atualizar(
        final UUID id,
        final @Nullable Nome nome,
        final @Nullable PrecoVenda precoVenda,
        final @Nullable Boolean requerDiagnostico
    ) {
        return this.operacoes.escrita("atualizar", () -> {
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
        this.operacoes.escrita("excluir", () -> encontrar(id, LockModeType.PESSIMISTIC_WRITE).excluir());
    }

    public InsumoObrigatorio associarInsumoObrigatorio(
        final UUID servicoId,
        final UUID insumoId,
        final Quantidade quantidade
    ) {
        return this.operacoes.escrita("associar_insumo_obrigatorio", () -> {
            encontrar(servicoId, LockModeType.PESSIMISTIC_WRITE);
            if (this.insumos.buscar(insumoId, LockModeType.PESSIMISTIC_WRITE).isEmpty()) {
                throw new InsumoNaoEncontradoException();
            }
            this.repositorio.associarInsumoObrigatorio(servicoId, insumoId, quantidade);
            return new InsumoObrigatorio(insumoId, quantidade);
        });
    }

    public Pagina<InsumoObrigatorio> listarInsumosObrigatorios(
        final UUID servicoId,
        final @Nullable UUID after,
        final Pagina.Limite limite
    ) {
        return this.operacoes.pagina("listar_insumos_obrigatorios", limite, () -> {
            encontrar(servicoId, LockModeType.NONE);
            return this.repositorio.listarInsumosObrigatorios(servicoId, after, limite);
        }, InsumoObrigatorio::insumoId);
    }

    public void removerInsumoObrigatorio(final UUID servicoId, final UUID insumoId) {
        this.operacoes.escrita("remover_insumo_obrigatorio", () -> {
            encontrar(servicoId, LockModeType.PESSIMISTIC_WRITE);
            if (!this.repositorio.removerInsumoObrigatorio(servicoId, insumoId)) {
                throw new InsumoObrigatorioNaoEncontradoException();
            }
        });
    }

    private Servico encontrar(final UUID id, final LockModeType modoBloqueio) {
        return this.repositorio.buscar(id, modoBloqueio).orElseThrow(ServicoNaoEncontradoException::new);
    }
}
