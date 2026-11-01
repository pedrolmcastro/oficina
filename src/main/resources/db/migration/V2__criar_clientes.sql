CREATE TABLE oficina.clientes (
    id UUID PRIMARY KEY CHECK (uuid_extract_version(id) = 7),
    nome TEXT NOT NULL CHECK (char_length(btrim(nome)) > 0),
    email TEXT NOT NULL CHECK (char_length(btrim(email)) > 0),
    tipo_documento TEXT NOT NULL CHECK (tipo_documento IN ('CPF', 'CNPJ')),
    documento_criptografado BYTEA NOT NULL CHECK (octet_length(documento_criptografado) > 29),
    hash_documento BYTEA NOT NULL CHECK (octet_length(hash_documento) = 32),
    deletado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX uk_clientes_documento
    ON oficina.clientes (tipo_documento, hash_documento) WHERE deletado = FALSE;
