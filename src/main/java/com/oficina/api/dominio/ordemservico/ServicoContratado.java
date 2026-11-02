package com.oficina.api.dominio.ordemservico;

import com.oficina.api.dominio.Entidade;
import com.oficina.api.dominio.servico.Nome;
import com.oficina.api.dominio.servico.PrecoVenda;
import com.oficina.api.dominio.servico.Servico;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "ordens_servico_servicos", schema = "oficina")
public class ServicoContratado extends Entidade {
    private UUID servicoId;
    @Embedded
    private Nome nome;
    @Embedded
    @AttributeOverride(name = "preco.valor", column = @Column(name = "preco", nullable = false, precision = 19,
        scale = 2))
    private PrecoVenda preco;
    private boolean requerDiagnostico;

    @SuppressWarnings(value = "NullAway.Init") // JPA preenche os campos após chamar este construtor
    protected ServicoContratado() {}

    public ServicoContratado(final Servico servico) {
        this.servicoId = servico.id();
        this.nome = servico.nome();
        this.preco = servico.precoVenda();
        this.requerDiagnostico = servico.requerDiagnostico();
    }

    public UUID servicoId() {
        return this.servicoId;
    }
    public Nome nome() {
        return this.nome;
    }
    public PrecoVenda preco() {
        return this.preco;
    }
    public boolean requerDiagnostico() {
        return this.requerDiagnostico;
    }
}
