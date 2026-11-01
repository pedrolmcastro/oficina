CREATE TABLE oficina.veiculos (
    id UUID PRIMARY KEY CHECK (uuid_extract_version(id) = 7),
    placa_criptografada BYTEA NOT NULL CHECK (octet_length(placa_criptografada) > 29),
    hash_placa BYTEA NOT NULL CHECK (octet_length(hash_placa) = 32),
    modelo TEXT NOT NULL CHECK (char_length(btrim(modelo)) > 0),
    marca TEXT NOT NULL CHECK (char_length(btrim(marca)) > 0),
    ano INTEGER NOT NULL
        CHECK (ano BETWEEN 1886 AND EXTRACT(YEAR FROM CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo')::INTEGER + 1),
    cliente_id UUID NOT NULL REFERENCES oficina.clientes (id),
    deletado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uk_veiculos_placa
    ON oficina.veiculos (hash_placa) WHERE deletado = FALSE;

CREATE INDEX idx_veiculos_cliente ON oficina.veiculos (cliente_id);
