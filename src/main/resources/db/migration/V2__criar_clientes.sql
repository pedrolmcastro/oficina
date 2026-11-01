CREATE TABLE oficina.clientes (
    id UUID PRIMARY KEY CHECK (uuid_extract_version(id) = 7),
    nome TEXT NOT NULL CHECK (btrim(nome) <> ''),
    email TEXT NOT NULL CHECK (btrim(email) <> ''),
    tipo_documento TEXT NOT NULL CHECK (tipo_documento IN ('CPF', 'CNPJ')),
    documento_criptografado BYTEA NOT NULL CHECK (octet_length(documento_criptografado) > 29),
    hash_documento BYTEA NOT NULL CHECK (octet_length(hash_documento) = 32),
    CONSTRAINT uk_clientes_documento UNIQUE (tipo_documento, hash_documento)
);
