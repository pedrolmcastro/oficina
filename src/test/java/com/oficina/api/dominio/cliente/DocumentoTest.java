package com.oficina.api.dominio.cliente;

import com.oficina.api.dominio.cliente.Documento.CNPJ;
import com.oficina.api.dominio.cliente.Documento.CPF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DocumentoTest {
    @Test
    void deveCriarCpf() {
        final var valor = "52998224725";

        final var documento = Documento.de(valor);

        assertThat(documento).isInstanceOf(CPF.class);
        assertThat(documento.valor()).isEqualTo(valor);
        assertThat(documento.tipo()).isEqualTo(Documento.Tipo.CPF);
    }

    @Test
    void deveCriarCnpj() {
        final var valor = "11222333000181";

        final var documento = Documento.de(valor);

        assertThat(documento).isInstanceOf(CNPJ.class);
        assertThat(documento.valor()).isEqualTo(valor);
        assertThat(documento.tipo()).isEqualTo(Documento.Tipo.CNPJ);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "1", "5299822472", "529982247255", "1122233300018", "112223330001811" })
    void deveRejeitarTamanhoInvalido(final String valor) {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> Documento.de(valor))
            .withMessage("Documento deve conter 11 (CPF) ou 14 (CNPJ) dígitos");
    }

    @Nested
    class Cpf {
        @ParameterizedTest
        @ValueSource(strings = { "52998224725", "12345678909", "11144477735" })
        void deveAceitarCpfValido(final String valor) {
            assertThat(new CPF(valor).valor()).isEqualTo(valor);
        }

        @ParameterizedTest
        @ValueSource(strings = { "ABCDEFGHIJK", "5299822472A", "5299822472 ", "529982247-5", "５２９９８２２４７２５" })
        void deveRejeitarCaracteresNaoNumericos(final String valor) {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CPF(valor))
                .withMessage("CPF deve conter apenas dígitos");
        }

        @ParameterizedTest
        @ValueSource(strings = { "00000000000", "11111111111", "99999999999" })
        void deveRejeitarDigitosIguais(final String valor) {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CPF(valor))
                .withMessage("CPF não pode conter todos os dígitos iguais");
        }

        @Test
        void deveRejeitarPrimeiroDigitoVerificadorInvalido() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CPF("52998224715"))
                .withMessage("CPF possui dígitos verificadores inválidos");
        }

        @Test
        void deveRejeitarSegundoDigitoVerificadorInvalido() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CPF("52998224724"))
                .withMessage("CPF possui dígitos verificadores inválidos");
        }

        @ParameterizedTest
        @ValueSource(strings = { "", "5", "5299822472", "529982247255", "529.982.247-25", " 52998224725 " })
        void deveRejeitarTamanhoInvalido(final String valor) {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CPF(valor))
                .withMessage("CPF deve conter exatamente 11 caracteres");
        }

        @Test
        void deveSensurarValor() {
            final var valor = "52998224725";
            assertThat(new CPF(valor).toString()).doesNotContain(valor);
        }
    }

    @Nested
    class Cnpj {
        @ParameterizedTest
        @ValueSource(strings = { "11222333000181", "04252011000110" })
        void deveAceitarCnpjValido(final String valor) {
            assertThat(new CNPJ(valor).valor()).isEqualTo(valor);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "12ABC34501DE35",
            "1122233300018A",
            "1122233300018 ",
            "112223330001-1",
            "１１２２２３３３０００１８１" })
        void deveRejeitarCaracteresNaoNumericos(final String valor) {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CNPJ(valor))
                .withMessage("CNPJ deve conter apenas dígitos");
        }

        @ParameterizedTest
        @ValueSource(strings = { "00000000000000", "11111111111111", "99999999999999" })
        void deveRejeitarDigitosIguais(final String valor) {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CNPJ(valor))
                .withMessage("CNPJ não pode conter todos os dígitos iguais");
        }

        @Test
        void deveRejeitarPrimeiroDigitoVerificadorInvalido() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CNPJ("11222333000171"))
                .withMessage("CNPJ possui dígitos verificadores inválidos");
        }

        @Test
        void deveRejeitarSegundoDigitoVerificadorInvalido() {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CNPJ("11222333000180"))
                .withMessage("CNPJ possui dígitos verificadores inválidos");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "",
            "1",
            "1122233300018",
            "112223330001811",
            "11.222.333/0001-81",
            " 11222333000181 " })
        void deveRejeitarTamanhoInvalido(final String valor) {
            assertThatIllegalArgumentException()
                .isThrownBy(() -> new CNPJ(valor))
                .withMessage("CNPJ deve conter exatamente 14 caracteres");
        }

        @Test
        void deveSensurarValor() {
            final var valor = "11222333000181";
            assertThat(new CNPJ(valor).toString()).doesNotContain(valor);
        }
    }
}
