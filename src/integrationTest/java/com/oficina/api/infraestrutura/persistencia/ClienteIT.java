package com.oficina.api.infraestrutura.persistencia;

import com.oficina.api.ConfiguracaoIntegration;
import com.oficina.api.dominio.cliente.Cliente;
import com.oficina.api.dominio.cliente.Documento.CPF;
import com.oficina.api.dominio.cliente.Email;
import com.oficina.api.dominio.cliente.Nome;
import com.oficina.api.infraestrutura.persistencia.cliente.ProtecaoDocumento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.function.Consumer;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
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
