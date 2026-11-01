package com.oficina.api.dominio.servico;

import com.oficina.api.dominio.Entidade;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "servicos", schema = "oficina")
@SQLRestriction(value = "deletado = FALSE")
public class Servico extends Entidade {
    @Column(name = "deletado", nullable = false)
    private boolean deletado;
    @Column(name = "requer_diagnostico", nullable = false)
    private boolean requerDiagnostico;
    @Embedded
    private Nome nome;
    @Embedded
    private PrecoVenda precoVenda;

    @SuppressWarnings(value = "NullAway.Init") // JPA preenche os campos após chamar este construtor
    protected Servico() {}

    public Servico(final Nome nome, final PrecoVenda precoVenda, final boolean requerDiagnostico) {
        this.nome = nome;
        this.precoVenda = precoVenda;
        this.requerDiagnostico = requerDiagnostico;
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

    public boolean requerDiagnostico() {
        return this.requerDiagnostico;
    }

    public void setRequerDiagnostico(final boolean requerDiagnostico) {
        this.requerDiagnostico = requerDiagnostico;
    }

    public void excluir() {
        this.deletado = true;
    }
}
