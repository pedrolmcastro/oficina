package com.oficina.api.dominio.cliente;

import com.oficina.api.dominio.DadoInvalidoException;
import com.oficina.api.dominio.Entidade;
import com.oficina.api.dominio.cliente.Documento.CNPJ;
import com.oficina.api.dominio.cliente.Documento.CPF;
import com.oficina.api.infraestrutura.persistencia.cliente.DocumentoPersistido;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "clientes", schema = "oficina")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_documento")
@SQLRestriction(value = "deletado = FALSE")
public abstract class Cliente extends Entidade {
    @Column(name = "deletado", nullable = false)
    private boolean deletado;

    @Embedded
    private Nome nome;

    @Embedded
    private Email email;

    @SuppressWarnings(value = "NullAway.Init") // JPA preenche nome e email após chamar este construtor
    protected Cliente() {}

    protected Cliente(final Nome nome, final Email email) {
        this.nome = nome;
        this.email = email;
    }

    public Nome nome() {
        return this.nome;
    }

    public Email email() {
        return this.email;
    }

    public abstract Documento documento();

    public void setNome(final Nome novoNome) {
        this.nome = novoNome;
    }

    public void setEmail(final Email novoEmail) {
        this.email = novoEmail;
    }

    public abstract void setDocumento(Documento documento);

    public void excluir() {
        this.deletado = true;
    }

    @Entity
    @DiscriminatorValue(value = "CPF")
    public static class PessoaFisica extends Cliente {
        @CompositeType(value = DocumentoPersistido.class)
        private CPF cpf;

        @SuppressWarnings(value = "NullAway.Init") // JPA preenche o CPF após chamar este construtor
        protected PessoaFisica() {}

        public PessoaFisica(final Nome nome, final Email email, final CPF documento) {
            super(nome, email);
            this.cpf = documento;
        }

        @Override
        public CPF documento() {
            return this.cpf;
        }

        @Override
        public void setDocumento(final Documento documento) {
            if (!(documento instanceof CPF novoCpf)) {
                throw new DadoInvalidoException("Pessoa física deve possuir CPF");
            }
            this.cpf = novoCpf;
        }
    }

    @Entity
    @DiscriminatorValue(value = "CNPJ")
    public static class PessoaJuridica extends Cliente {
        @CompositeType(value = DocumentoPersistido.class)
        private CNPJ cnpj;

        @SuppressWarnings(value = "NullAway.Init") // JPA preenche o CNPJ após chamar este construtor
        protected PessoaJuridica() {}

        public PessoaJuridica(final Nome nome, final Email email, final CNPJ documento) {
            super(nome, email);
            this.cnpj = documento;
        }

        @Override
        public CNPJ documento() {
            return this.cnpj;
        }

        @Override
        public void setDocumento(final Documento documento) {
            if (!(documento instanceof CNPJ novoCnpj)) {
                throw new DadoInvalidoException("Pessoa jurídica deve possuir CNPJ");
            }
            this.cnpj = novoCnpj;
        }
    }
}
