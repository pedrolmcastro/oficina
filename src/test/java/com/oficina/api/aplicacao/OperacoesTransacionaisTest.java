package com.oficina.api.aplicacao;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperacoesTransacionaisTest {
    private final PlatformTransactionManager transacoes = mock(PlatformTransactionManager.class);
    private final SimpleTransactionStatus transacao = new SimpleTransactionStatus();
    private final OperacoesTransacionais operacoes = new OperacoesTransacionais(
        "teste.operacao",
        ObservationRegistry.NOOP,
        this.transacoes
    );

    @BeforeEach
    void configurarTransacao() {
        when(this.transacoes.getTransaction(any())).thenReturn(this.transacao);
    }

    @Test
    void deveExecutarLeituraEmTransacaoSomenteLeitura() {
        assertThat(this.operacoes.leitura("buscar", () -> "encontrado")).isEqualTo("encontrado");
        verify(this.transacoes).getTransaction(argThat(definicao -> definicao != null && definicao.isReadOnly()));
    }

    @Test
    void deveExecutarEscritaComRetorno() {
        assertThat(this.operacoes.escrita("criar", () -> "criado")).isEqualTo("criado");
        verify(this.transacoes).getTransaction(argThat(definicao -> definicao != null && !definicao.isReadOnly()));
    }
}
