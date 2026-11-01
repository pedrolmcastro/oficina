package com.oficina.api.dominio.cliente;

import com.oficina.api.dominio.DadoInvalidoException;

public sealed interface Documento {
    public abstract String valor();

    public abstract Tipo tipo();

    public static Documento de(final String valor) {
        return switch (valor.length()) {
            case 11 -> new CPF(valor);
            case 14 -> new CNPJ(valor);
            default -> throw new DadoInvalidoException("Documento deve conter 11 (CPF) ou 14 (CNPJ) dígitos");
        };
    }

    public static enum Tipo {
        CPF,
        CNPJ
    }

    public static record CPF(String valor) implements Documento {
        public CPF {
            if (valor.length() != 11) {
                throw new DadoInvalidoException("CPF deve conter exatamente 11 caracteres");
            }
            if (valor.chars().anyMatch(c -> c < '0' || c > '9')) {
                throw new DadoInvalidoException("CPF deve conter apenas dígitos");
            }
            if (valor.chars().distinct().count() == 1) {
                throw new DadoInvalidoException("CPF não pode conter todos os dígitos iguais");
            }
            if (digito(valor, 9) != valor.charAt(9) - '0' || digito(valor, 10) != valor.charAt(10) - '0') {
                throw new DadoInvalidoException("CPF possui dígitos verificadores inválidos");
            }
        }

        @Override
        public Tipo tipo() {
            return Tipo.CPF;
        }

        @Override
        public String toString() {
            return "CPF[valor=*]";
        }

        private static int digito(final String valor, final int tamanho) {
            var soma = 0;

            for (var i = 0; i < tamanho; i++) {
                soma += (valor.charAt(i) - '0') * (tamanho + 1 - i);
            }

            final var resto = soma % 11;

            return resto < 2
                ? 0
                : (11 - resto);
        }
    }

    public static record CNPJ(String valor) implements Documento {
        public CNPJ {
            if (valor.length() != 14) {
                throw new DadoInvalidoException("CNPJ deve conter exatamente 14 caracteres");
            }
            if (valor.chars().anyMatch(c -> c < '0' || c > '9')) {
                throw new DadoInvalidoException("CNPJ deve conter apenas dígitos");
            }
            if (valor.chars().distinct().count() == 1) {
                throw new DadoInvalidoException("CNPJ não pode conter todos os dígitos iguais");
            }
            if (digito(valor, 12) != valor.charAt(12) - '0' || digito(valor, 13) != valor.charAt(13) - '0') {
                throw new DadoInvalidoException("CNPJ possui dígitos verificadores inválidos");
            }
        }

        @Override
        public Tipo tipo() {
            return Tipo.CNPJ;
        }

        @Override
        public String toString() {
            return "CNPJ[valor=*]";
        }

        private static int digito(final String valor, final int tamanho) {
            var soma = 0;
            var peso = 2;

            for (var i = tamanho - 1; i >= 0; i--) {
                soma += (valor.charAt(i) - '0') * peso;
                peso = peso == 9
                    ? 2
                    : (peso + 1);
            }

            final var resto = soma % 11;

            return resto < 2
                ? 0
                : (11 - resto);
        }
    }
}
