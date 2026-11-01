package com.oficina.api.dominio.cliente;

import com.oficina.api.dominio.cliente.Cliente.PessoaFisica;
import com.oficina.api.dominio.cliente.Cliente.PessoaJuridica;
import com.oficina.api.dominio.cliente.Documento.CNPJ;
import com.oficina.api.dominio.cliente.Documento.CPF;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ClienteTest {
    @Test
    void deveAceitarPessoaFisica() {
        final var nome = new Nome("Nome");
        final var email = new Email("nome@example.com");
        final var cpf = new CPF("52998224725");

        final var cliente = new PessoaFisica(nome, email, cpf);

        assertThat(cliente.nome()).isEqualTo(nome);
        assertThat(cliente.email()).isEqualTo(email);
        assertThat(cliente.documento()).isEqualTo(cpf);
    }

    @Test
    void deveAceitarPessoaJuridica() {
        final var nome = new Nome("Nome");
        final var email = new Email("nome@example.com");
        final var cnpj = new CNPJ("11222333000181");

        final var cliente = new PessoaJuridica(nome, email, cnpj);

        assertThat(cliente.nome()).isEqualTo(nome);
        assertThat(cliente.email()).isEqualTo(email);
        assertThat(cliente.documento()).isEqualTo(cnpj);
    }
}
