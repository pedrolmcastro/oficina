CREATE TABLE oficina.insumos (
    id UUID PRIMARY KEY CHECK (uuid_extract_version(id) = 7),
    nome TEXT NOT NULL CHECK (char_length(btrim(nome)) > 0),
    preco_venda NUMERIC(19, 2) NOT NULL CHECK (preco_venda >= 0)
        CONSTRAINT ck_insumos_preco_venda_maximo CHECK (preco_venda <= 10000.00),
    deletado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uk_insumos_nome ON oficina.insumos (nome) WHERE deletado = FALSE;

CREATE INDEX idx_insumos_nome_busca ON oficina.insumos
    USING GIN (to_tsvector('portuguese', nome)) WHERE deletado = FALSE;
