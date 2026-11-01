package com.oficina.api.dominio.cliente;

import com.oficina.api.dominio.Entidade;
import com.oficina.api.infraestrutura.persistencia.cliente.DocumentoPersistido;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "clientes", schema = "oficina")
@SQLRestriction(value = "deletado = FALSE")
public class Cliente extends Entidade {
    @Column(name = "deletado", nullable = false)
    private boolean deletado;

    @Embedded
    private Nome nome;

    @Embedded
    private Email email;

    @AttributeOverrides(value = {
        @AttributeOverride(name = "cifrado", column = @Column(name = "documento_criptografado", nullable = false)),
        @AttributeOverride(name = "hash", column = @Column(name = "hash_documento", nullable = false)) })
    @CompositeType(value = DocumentoPersistido.class)
    private Documento documento;

    @SuppressWarnings(value = "NullAway.Init") // JPA preenche os campos após chamar este construtor
    protected Cliente() {}

    public Cliente(final Nome nome, final Email email, final Documento documento) {
        this.nome = nome;
        this.email = email;
        this.documento = documento;
    }

    public Nome nome() {
        return this.nome;
    }

    public Email email() {
        return this.email;
    }

    public Documento documento() {
        return this.documento;
    }

    public void setNome(final Nome novoNome) {
        this.nome = novoNome;
    }

    public void setEmail(final Email novoEmail) {
        this.email = novoEmail;
    }

    public void setDocumento(final Documento novoDocumento) {
        this.documento = novoDocumento;
    }

    public void excluir() {
        this.deletado = true;
    }
}
