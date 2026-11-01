CREATE TABLE oficina.servicos (
    id UUID PRIMARY KEY CHECK (uuid_extract_version(id) = 7),
    nome TEXT NOT NULL CHECK (char_length(btrim(nome)) > 0),
    preco_venda NUMERIC(19, 2) NOT NULL CHECK (preco_venda >= 0 AND preco_venda <= 10000.00),
    requer_diagnostico BOOLEAN NOT NULL,
    deletado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uk_servicos_nome ON oficina.servicos (nome) WHERE deletado = FALSE;

CREATE INDEX idx_servicos_nome_busca ON oficina.servicos
    USING GIN (to_tsvector('portuguese', nome)) WHERE deletado = FALSE;

CREATE TABLE oficina.servicos_insumos_obrigatorios (
    servico_id UUID NOT NULL REFERENCES oficina.servicos (id),
    insumo_id UUID NOT NULL REFERENCES oficina.insumos (id),
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    PRIMARY KEY (servico_id, insumo_id)
);

CREATE INDEX idx_servicos_insumos_obrigatorios_insumo ON oficina.servicos_insumos_obrigatorios (insumo_id);
