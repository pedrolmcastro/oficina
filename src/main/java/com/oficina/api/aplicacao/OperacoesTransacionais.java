package com.oficina.api.aplicacao;

import com.oficina.api.dominio.Entidade;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public final class OperacoesTransacionais {
    private final String nomeObservacao;
    private final ObservationRegistry observacoes;
    private final TransactionTemplate leitura;
    private final TransactionTemplate escrita;

    public OperacoesTransacionais(
        final String nomeObservacao,
        final ObservationRegistry observacoes,
        final PlatformTransactionManager transacoes
    ) {
        this.nomeObservacao = nomeObservacao;
        this.observacoes = observacoes;
        this.leitura = new TransactionTemplate(transacoes);
        this.leitura.setReadOnly(true);
        this.escrita = new TransactionTemplate(transacoes);
    }

    public <T> T leitura(final String operacao, final Supplier<T> acao) {
        return executar(operacao, this.leitura, acao);
    }

    public <T extends Entidade> Pagina<T> pagina(
        final String operacao,
        final Pagina.Limite limite,
        final Supplier<List<T>> consulta
    ) {
        return leitura(operacao, () -> Pagina.de(consulta.get(), limite));
    }

    public <T> T escrita(final String operacao, final Supplier<T> acao) {
        return executar(operacao, this.escrita, acao);
    }

    public void escrita(final String operacao, final Runnable acao) {
        escrita(operacao, () -> {
            acao.run();
            return Boolean.TRUE;
        });
    }

    private <T> T executar(final String operacao, final TransactionTemplate transacao, final Supplier<T> acao) {
        return Objects
            .requireNonNull(
                Observation
                    .createNotStarted(this.nomeObservacao, this.observacoes)
                    .lowCardinalityKeyValue("operacao", operacao)
                    .observe(() -> transacao.execute(_ -> acao.get()))
            );
    }
}
