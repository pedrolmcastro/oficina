package com.oficina.api.dominio.ordemservico;

import com.oficina.api.dominio.DadoInvalidoException;
import com.oficina.api.dominio.Preco;
import com.oficina.api.dominio.Quantidade;
import com.oficina.api.dominio.TextoNormalizado;
import com.oficina.api.dominio.insumo.Insumo;
import com.oficina.api.dominio.servico.Nome;
import com.oficina.api.dominio.servico.PrecoVenda;
import com.oficina.api.dominio.servico.Servico;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdemServicoTest {
    @Test
    void deveExigirServicosSemDuplicatas() {
        final var servico = contratado();
        assertThatThrownBy(() -> criar(List.of())).isInstanceOf(DadoInvalidoException.class);
        final var servicos = List.of(servico, servico);
        assertThatThrownBy(() -> criar(servicos)).isInstanceOf(DadoInvalidoException.class);
    }

    @Test
    void deveExigirQueOrigemDoInsumoPertencaAOrdem() {
        final var ordem = criar(List.of(contratado()));
        final var insumo = new Insumo(
            new com.oficina.api.dominio.insumo.Nome(new TextoNormalizado("Óleo")),
            new com.oficina.api.dominio.insumo.PrecoVenda(new Preco(BigDecimal.TEN))
        );
        final var servicoContratadoId = UUID.randomUUID();
        final var quantidade = new Quantidade(1);
        assertThatThrownBy(() -> ordem.associarObrigatorio(insumo, servicoContratadoId, quantidade))
            .isInstanceOf(DadoInvalidoException.class);
        assertThat(ordem.insumos()).isEmpty();
    }

    @Test
    void deveSepararTokensERevogarDecisaoAoRenovarAcompanhamento() {
        final var ordem = criar(List.of(contratado()));
        assertThat(ordem.hashTokenDecisao()).isNull();
        assertThat(ordem.decisaoExpiraEm()).isNull();
        final var validade = Instant.now().plusSeconds(60);
        assertThatThrownBy(() -> ordem.emitirAcessoDecisao("hash-decisao", validade))
            .isInstanceOf(com.oficina.api.dominio.ordemservico.ConflitoOrdemServicoException.class);
        ordem.iniciarDiagnostico("admin");
        ordem.concluirDiagnostico("admin");
        ordem.emitirAcessoDecisao("hash-decisao", validade);
        assertThat(ordem.hashTokenDecisao()).isEqualTo("hash-decisao");
        assertThat(ordem.decisaoExpiraEm()).isEqualTo(validade);
        assertThat(ordem.hashToken()).isEqualTo("hash");
        ordem.renovarAcesso("novo-hash", validade, "admin");
        assertThat(ordem.hashToken()).isEqualTo("novo-hash");
        assertThat(ordem.tokenExpiraEm()).isEqualTo(validade);
        assertThat(ordem.hashTokenDecisao()).isNull();
        assertThat(ordem.decisaoExpiraEm()).isNull();
    }

    @Test
    void deveRecusarReservaAcimaDaNecessidadeSemAlterarItem() {
        final var contratado = contratado();
        final var ordem = criar(List.of(contratado));
        final var insumo = new Insumo(
            new com.oficina.api.dominio.insumo.Nome(new TextoNormalizado("Óleo")),
            new com.oficina.api.dominio.insumo.PrecoVenda(new Preco(BigDecimal.TEN))
        );
        ordem.associarObrigatorio(insumo, contratado.id(), new Quantidade(2));
        ordem.iniciarDiagnostico("admin");
        ordem.concluirDiagnostico("admin");
        ordem.aprovar();
        final var item = ordem.insumos().getFirst();
        final var quantidade = new Quantidade(3);
        assertThatThrownBy(() -> ordem.reservarInsumo(item.id(), quantidade))
            .isInstanceOf(ConflitoOrdemServicoException.class);
        assertThat(item.quantidadeReservada()).isZero();
        assertThat(item.quantidadePendente()).isEqualTo(2);
    }

    private static ServicoContratado contratado() {
        return new ServicoContratado(
            new Servico(new Nome(new TextoNormalizado("Revisão")), new PrecoVenda(new Preco(BigDecimal.TEN)), true)
        );
    }

    private static OrdemServico criar(final List<ServicoContratado> servicos) {
        return OrdemServico.criar(UUID.randomUUID(), UUID.randomUUID(), servicos, "hash", Instant.now(), "admin");
    }
}
