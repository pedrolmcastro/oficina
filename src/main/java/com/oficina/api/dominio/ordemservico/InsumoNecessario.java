package com.oficina.api.dominio.ordemservico;

import com.oficina.api.dominio.Entidade;
import com.oficina.api.dominio.Quantidade;
import com.oficina.api.dominio.insumo.Insumo;
import com.oficina.api.dominio.insumo.Nome;
import com.oficina.api.dominio.insumo.PrecoVenda;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "ordens_servico_insumos", schema = "oficina")
public class InsumoNecessario extends Entidade {
    private int quantidadeReservada;
    private int quantidadeConsumida;
    private UUID insumoId;
    private @Nullable UUID servicoContratadoId;
    @Embedded
    private Nome nome;
    @Embedded
    @AttributeOverride(name = "preco.valor", column = @Column(name = "preco_venda", nullable = false, precision = 19,
        scale = 2))
    private PrecoVenda precoVenda;
    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "quantidade", nullable = false))
    private Quantidade quantidade;

    @SuppressWarnings(value = "NullAway.Init") // JPA preenche os campos após chamar este construtor
    protected InsumoNecessario() {}

    public InsumoNecessario(
        final Insumo insumo,
        final @Nullable UUID servicoContratadoId,
        final Quantidade quantidade
    ) {
        this.insumoId = insumo.id();
        this.servicoContratadoId = servicoContratadoId;
        this.nome = insumo.nome();
        this.precoVenda = insumo.precoVenda();
        this.quantidade = quantidade;
    }

    public UUID insumoId() {
        return this.insumoId;
    }
    public @Nullable UUID servicoContratadoId() {
        return this.servicoContratadoId;
    }
    public Nome nome() {
        return this.nome;
    }
    public PrecoVenda precoVenda() {
        return this.precoVenda;
    }
    public Quantidade quantidade() {
        return this.quantidade;
    }
    public boolean avulso() {
        return this.servicoContratadoId == null;
    }
    public BigDecimal subtotal() {
        return this.precoVenda.preco().valor().multiply(BigDecimal.valueOf(this.quantidade.valor()));
    }

    public int quantidadeReservada() {
        return this.quantidadeReservada;
    }

    public int quantidadeConsumida() {
        return this.quantidadeConsumida;
    }

    public int quantidadePendente() {
        return this.quantidade.valor() - this.quantidadeReservada - this.quantidadeConsumida;
    }

    void reservar(final Quantidade quantidade) {
        if (quantidade.valor() > quantidadePendente()) {
            throw new ConflitoOrdemServicoException("Reserva excede a necessidade do insumo necessário");
        }
        this.quantidadeReservada += quantidade.valor();
    }

    void consumirReserva() {
        this.quantidadeConsumida += this.quantidadeReservada;
        this.quantidadeReservada = 0;
    }

    void setQuantidade(final Quantidade novaQuantidade) {
        exigirAvulso();
        this.quantidade = novaQuantidade;
    }

    void exigirAvulso() {
        if (!avulso()) {
            throw new ConflitoOrdemServicoException("Insumos associados por serviços são imutáveis");
        }
    }
}
