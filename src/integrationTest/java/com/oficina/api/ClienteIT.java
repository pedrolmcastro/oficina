package com.oficina.api;

import com.oficina.api.dominio.cliente.Cliente;
import com.oficina.api.dominio.cliente.Documento.CNPJ;
import com.oficina.api.dominio.cliente.Documento.CPF;
import com.oficina.api.dominio.cliente.Email;
import com.oficina.api.dominio.cliente.Nome;
import com.oficina.api.infraestrutura.persistencia.cliente.ProtecaoDocumento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(value = ConfiguracaoIntegration.class)
class ClienteIT {
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ProtecaoDocumento protecao;

    @ParameterizedTest
    @MethodSource(value = "clientes")
    void devePersistirCliente(final Cliente cliente) {
        emTransacao(em -> {
            em.persist(cliente);
            em.flush();
            em.clear();
            final var recarregado = em.find(Cliente.class, cliente.id());
            assertThat(recarregado.id()).isEqualTo(cliente.id());
            assertThat(recarregado.documento()).isEqualTo(cliente.documento());
            assertThat(recarregado.nome()).isEqualTo(cliente.nome());
            assertThat(recarregado.email()).isEqualTo(cliente.email());
            final var cifrado = (byte[]) em
                .createNativeQuery("SELECT documento_criptografado FROM oficina.clientes WHERE id = :id", byte[].class)
                .setParameter("id", cliente.id())
                .getSingleResult();
            assertThat(this.protecao.descriptografar(cifrado)).isEqualTo(cliente.documento());
            final var encontrado = em
                .createNativeQuery("SELECT id FROM oficina.clientes WHERE hash_documento = :hash", UUID.class)
                .setParameter("hash", this.protecao.calcularHash(cliente.documento()))
                .getSingleResult();
            assertThat(encontrado).isEqualTo(cliente.id());
        });
    }

    @Test
    void deveRejeitarDocumentoDuplicado() {
        assertThatThrownBy(() -> emTransacao(em -> {
            em.persist(new Cliente(new Nome("Maria"), new Email("maria@example.com"), new CPF("52998224725")));
            em.persist(new Cliente(new Nome("Outra"), new Email("outra@example.com"), new CPF("52998224725")));
            em.flush();
        }))
            .isExactlyInstanceOf(ConstraintViolationException.class)
            .satisfies(
                erro -> assertThat(((ConstraintViolationException) erro).getConstraintName())
                    .isEqualTo("uk_clientes_documento")
            );
    }

    @Test
    void deveRejeitarHashIncompativel() {
        emTransacao(em -> {
            final var cliente = new Cliente(new Nome("Maria"), new Email("maria@example.com"), new CPF("52998224725"));

            em.persist(cliente);
            em.flush();
            em
                .createNativeQuery("UPDATE oficina.clientes SET hash_documento = :hash WHERE id = :id")
                .setParameter("id", cliente.id())
                .setParameter("hash", this.protecao.calcularHash(new CPF("11144477735")))
                .executeUpdate();
            em.clear();

            final var id = cliente.id();

            assertThatThrownBy(() -> em.find(Cliente.class, id))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("HMAC incompatível com o documento persistido");
        });
    }

    private static Stream<Cliente> clientes() {
        return Stream
            .of(
                new Cliente(new Nome("Maria"), new Email("maria@example.com"), new CPF("52998224725")),
                new Cliente(new Nome("Oficina"), new Email("oficina@example.com"), new CNPJ("11222333000181"))
            );
    }

    private void emTransacao(final Consumer<EntityManager> acao) {
        try (final var em = this.entityManagerFactory.createEntityManager()) {
            final var transacao = em.getTransaction();
            transacao.begin();
            try {
                acao.accept(em);
            } finally {
                transacao.rollback();
            }
        }
    }
}
