package com.oficina.api.aplicacao.insumo;

import com.oficina.api.aplicacao.OperacoesTransacionais;
import com.oficina.api.aplicacao.Pagina;
import com.oficina.api.dominio.insumo.Insumo;
import com.oficina.api.dominio.insumo.Nome;
import com.oficina.api.dominio.insumo.PrecoVenda;
import com.oficina.api.infraestrutura.persistencia.insumo.RepositorioInsumos;
import io.micrometer.observation.ObservationRegistry;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

@Service
public class ServicoInsumos {
    private final RepositorioInsumos repositorio;
    private final OperacoesTransacionais operacoes;

    public ServicoInsumos(
        final RepositorioInsumos repositorio,
        final ObservationRegistry observacoes,
        final PlatformTransactionManager transacoes
    ) {
        this.repositorio = repositorio;
        this.operacoes = new OperacoesTransacionais("insumos.operacao", observacoes, transacoes);
    }

    public Insumo criar(final Nome nome, final PrecoVenda precoVenda) {
        return this.operacoes.escrita("criar", () -> {
            final var insumo = new Insumo(nome, precoVenda);
            this.repositorio.adicionar(insumo);
            return insumo;
        });
    }

    public Insumo buscar(final UUID id) {
        return this.operacoes.leitura("buscar_id", () -> encontrar(id, LockModeType.NONE));
    }

    public Pagina<Insumo> listar(final @Nullable String nome, final @Nullable UUID after, final Pagina.Limite limite) {
        return this.operacoes.pagina("listar", limite, () -> this.repositorio.listar(nome, after, limite));
    }

    public Insumo atualizar(final UUID id, final @Nullable Nome nome, final @Nullable PrecoVenda precoVenda) {
        return this.operacoes.escrita("atualizar", () -> {
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
        this.operacoes.escrita("excluir", () -> {
            final var insumo = encontrar(id, LockModeType.PESSIMISTIC_WRITE);
            if (this.repositorio.obrigatorioParaServicoAtivo(id)) {
                throw new DataIntegrityViolationException("Insumo obrigatório de serviço ativo");
            }
            insumo.excluir();
        });
    }

    private Insumo encontrar(final UUID id, final LockModeType modoBloqueio) {
        return this.repositorio.buscar(id, modoBloqueio).orElseThrow(InsumoNaoEncontradoException::new);
    }
}
