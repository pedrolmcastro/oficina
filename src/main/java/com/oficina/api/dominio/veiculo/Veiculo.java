package com.oficina.api.dominio.veiculo;

import com.oficina.api.dominio.Entidade;
import com.oficina.api.infraestrutura.persistencia.veiculo.PlacaPersistida;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "veiculos", schema = "oficina")
@SQLRestriction(value = "deletado = FALSE")
public class Veiculo extends Entidade {
    @Column(name = "deletado", nullable = false)
    private boolean deletado;

    @AttributeOverrides(value = {
        @AttributeOverride(name = "cifrado", column = @Column(name = "placa_criptografada", nullable = false)),
        @AttributeOverride(name = "hash", column = @Column(name = "hash_placa", nullable = false)) })
    @CompositeType(value = PlacaPersistida.class)
    private Placa placa;

    @Embedded
    private Modelo modelo;

    @Embedded
    private Marca marca;

    @Embedded
    private Ano ano;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @SuppressWarnings(value = "NullAway.Init") // JPA preenche os campos após chamar este construtor
    protected Veiculo() {}

    public Veiculo(final Placa placa, final Modelo modelo, final Marca marca, final Ano ano, final UUID clienteId) {
        this.placa = placa;
        this.modelo = modelo;
        this.marca = marca;
        this.ano = ano;
        this.clienteId = clienteId;
    }

    public Placa placa() {
        return this.placa;
    }

    public Modelo modelo() {
        return this.modelo;
    }

    public Marca marca() {
        return this.marca;
    }

    public Ano ano() {
        return this.ano;
    }

    public UUID clienteId() {
        return this.clienteId;
    }

    public void setPlaca(final Placa placa) {
        this.placa = placa;
    }

    public void setModelo(final Modelo modelo) {
        this.modelo = modelo;
    }

    public void setMarca(final Marca marca) {
        this.marca = marca;
    }

    public void setAno(final Ano ano) {
        this.ano = ano;
    }

    public void setClienteId(final UUID clienteId) {
        this.clienteId = clienteId;
    }

    public void excluir() {
        this.deletado = true;
    }
}
