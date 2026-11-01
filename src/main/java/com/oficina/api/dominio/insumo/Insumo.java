package com.oficina.api.dominio.insumo;

import com.oficina.api.dominio.Entidade;
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

    public void excluir() {
        this.deletado = true;
    }
}
