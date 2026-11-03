#!/bin/sh

set -eu
umask 077

for chave in CLIENTES_CHAVE_CRIPTOGRAFIA CLIENTES_CHAVE_HASH VEICULOS_CHAVE_CRIPTOGRAFIA VEICULOS_CHAVE_HASH JWT_SEGREDO; do
    if [ ! -s "/segredos/$chave" ]; then
        dd if=/dev/urandom bs=32 count=1 2>/dev/null | base64 > "/segredos/$chave.tmp"
        mv "/segredos/$chave.tmp" "/segredos/$chave"
    fi
done

chown -R 10001:10001 /segredos
chmod 700 /segredos
