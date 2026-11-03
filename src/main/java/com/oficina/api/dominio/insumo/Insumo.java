package com.oficina.api.dominio.insumo;

import com.oficina.api.dominio.DadoInvalidoException;
import com.oficina.api.dominio.Entidade;
import com.oficina.api.dominio.Quantidade;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "insumos", schema = "oficina")
@SQLRestriction(value = "deletado = FALSE")
public class Insumo extends Entidade {
    @Column(name = "deletado", nullable = false)
    private boolean deletado;
    private int quantidadeEstoqueFisico;
    private int quantidadeReservada;
    @Embedded
    private Nome nome;
    @Embedded
    private PrecoVenda precoVenda;

    @SuppressWarnings(value = "NullAway.Init") // JPA preenche os campos após chamar este construtor
    protected Insumo() {}

    public Insumo(final Nome nome, final PrecoVenda precoVenda) {
        this.nome = nome;
        this.precoVenda = precoVenda;
    }

    public Nome nome() {
        return this.nome;
    }

    public PrecoVenda precoVenda() {
        return this.precoVenda;
    }

    public void setNome(final Nome nome) {
        this.nome = nome;
    }

    public void setPrecoVenda(final PrecoVenda precoVenda) {
        this.precoVenda = precoVenda;
    }

    public int quantidadeEstoqueFisico() {
        return this.quantidadeEstoqueFisico;
    }

    public int quantidadeReservada() {
        return this.quantidadeReservada;
    }

    public int quantidadeDisponivel() {
        return this.quantidadeEstoqueFisico - this.quantidadeReservada;
    }

    public void reporEstoque(final Quantidade quantidade) {
        if (quantidade.valor() > Integer.MAX_VALUE - this.quantidadeEstoqueFisico) {
            throw new DadoInvalidoException("Estoque excede o limite permitido");
        }
        this.quantidadeEstoqueFisico += quantidade.valor();
    }

    public void reservar(final Quantidade quantidade) {
        if (quantidade.valor() > quantidadeDisponivel()) {
            throw new DadoInvalidoException("Estoque disponível insuficiente");
        }
        this.quantidadeReservada += quantidade.valor();
    }

    public void consumirReserva(final Quantidade quantidade) {
        if (quantidade.valor() > this.quantidadeReservada) {
            throw new DadoInvalidoException("Quantidade excede o estoque reservado");
        }
        this.quantidadeReservada -= quantidade.valor();
        this.quantidadeEstoqueFisico -= quantidade.valor();
    }

    public void excluir() {
        this.deletado = true;
    }
}
