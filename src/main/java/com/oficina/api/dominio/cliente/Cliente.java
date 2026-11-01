package com.oficina.api.dominio.cliente;

import com.oficina.api.dominio.Entidade;
import com.oficina.api.dominio.cliente.Documento.CNPJ;
import com.oficina.api.dominio.cliente.Documento.CPF;
import com.oficina.api.infraestrutura.persistencia.cliente.DocumentoPersistido;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import org.hibernate.annotations.CompositeType;

@Entity
@Table(name = "clientes", schema = "oficina")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_documento")
public abstract class Cliente extends Entidade {
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
    }
}
