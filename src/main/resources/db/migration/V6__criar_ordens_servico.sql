CREATE TABLE oficina.ordens_servico (
    id UUID PRIMARY KEY CHECK (uuid_extract_version(id) = 7),
    cliente_id UUID NOT NULL REFERENCES oficina.clientes (id),
    veiculo_id UUID NOT NULL REFERENCES oficina.veiculos (id),
    status TEXT NOT NULL CHECK (
        status IN (
            'AGUARDANDO_DIAGNOSTICO',
            'EM_DIAGNOSTICO',
            'AGUARDANDO_CONFIRMACAO',
            'AGUARDANDO_INSUMOS',
            'AGUARDANDO_EXECUCAO',
            'EM_EXECUCAO',
            'AGUARDANDO_RETIRADA',
            'ENTREGUE'
        )
    ),
    resultado TEXT CHECK (resultado IN ('ORCAMENTO_REJEITADO', 'EXECUCAO_CONCLUIDA')),
    criada_em TIMESTAMPTZ NOT NULL,
    hash_token TEXT NOT NULL UNIQUE,
    token_expira_em TIMESTAMPTZ NOT NULL,
    hash_token_decisao TEXT UNIQUE,
    decisao_expira_em TIMESTAMPTZ,
    CONSTRAINT ck_token_decisao CHECK (
        (hash_token_decisao IS NULL) = (decisao_expira_em IS NULL)
    )
);

CREATE INDEX idx_ordens_servico_status_id ON oficina.ordens_servico (status, id);
CREATE INDEX idx_ordens_servico_fila_insumos ON oficina.ordens_servico (criada_em, id)
    WHERE status = 'AGUARDANDO_INSUMOS';
CREATE INDEX idx_ordens_servico_cliente ON oficina.ordens_servico (cliente_id);
CREATE INDEX idx_ordens_servico_veiculo ON oficina.ordens_servico (veiculo_id);

CREATE TABLE oficina.ordens_servico_servicos (
    id UUID PRIMARY KEY CHECK (uuid_extract_version(id) = 7),
    ordem_servico_id UUID NOT NULL REFERENCES oficina.ordens_servico (id),
    servico_id UUID NOT NULL REFERENCES oficina.servicos (id),
    nome TEXT NOT NULL,
    preco_venda NUMERIC(19, 2) NOT NULL CHECK (preco_venda >= 0),
    requer_diagnostico BOOLEAN NOT NULL,
    UNIQUE (ordem_servico_id, servico_id),
    UNIQUE (ordem_servico_id, id)
);

CREATE INDEX idx_ordens_servico_servicos_servico ON oficina.ordens_servico_servicos (servico_id);

CREATE TABLE oficina.ordens_servico_insumos (
    id UUID PRIMARY KEY CHECK (uuid_extract_version(id) = 7),
    ordem_servico_id UUID NOT NULL REFERENCES oficina.ordens_servico (id),
    servico_contratado_id UUID,
    insumo_id UUID NOT NULL REFERENCES oficina.insumos (id),
    nome TEXT NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    quantidade_reservada INTEGER NOT NULL DEFAULT 0 CHECK (quantidade_reservada >= 0),
    quantidade_consumida INTEGER NOT NULL DEFAULT 0 CHECK (quantidade_consumida >= 0),
    CHECK (quantidade_reservada + quantidade_consumida <= quantidade),
    preco_venda NUMERIC(19, 2) NOT NULL CHECK (preco_venda >= 0),
    FOREIGN KEY (ordem_servico_id, servico_contratado_id)
        REFERENCES oficina.ordens_servico_servicos (ordem_servico_id, id) DEFERRABLE INITIALLY DEFERRED,
    UNIQUE NULLS NOT DISTINCT (ordem_servico_id, servico_contratado_id, insumo_id)
);

CREATE INDEX idx_ordens_servico_insumos_insumo ON oficina.ordens_servico_insumos (insumo_id);

CREATE TABLE oficina.ordens_servico_eventos (
    ordem_servico_id UUID NOT NULL REFERENCES oficina.ordens_servico (id),
    sequencia INTEGER NOT NULL CHECK (sequencia >= 0),
    tipo TEXT NOT NULL,
    ocorrido_em TIMESTAMPTZ NOT NULL,
    autor TEXT NOT NULL,
    dados JSONB NOT NULL,
    PRIMARY KEY (ordem_servico_id, sequencia)
);
