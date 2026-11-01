package com.oficina.api.infraestrutura.persistencia;

import com.oficina.api.ConfiguracaoIntegration;
import com.oficina.api.dominio.cliente.Cliente;
import com.oficina.api.dominio.cliente.Documento.CPF;
import com.oficina.api.dominio.cliente.Email;
import com.oficina.api.dominio.cliente.Nome;
import com.oficina.api.dominio.veiculo.Ano;
import com.oficina.api.dominio.veiculo.Marca;
import com.oficina.api.dominio.veiculo.Modelo;
import com.oficina.api.dominio.veiculo.Placa;
import com.oficina.api.dominio.veiculo.Veiculo;
import com.oficina.api.infraestrutura.persistencia.veiculo.ProtecaoPlaca;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.Year;
import java.time.ZoneId;
import java.util.UUID;
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
class VeiculoIT {
    @Autowired
    private EntityManagerFactory fabrica;
    @Autowired
    private ProtecaoPlaca protecao;

    @Test
    void deveRejeitarPlacaDuplicada() {
        assertThatThrownBy(() -> emTransacao(em -> {
            final var cliente = cliente(em);
            em.persist(veiculo("ABC1234", cliente.id()));
            em.persist(veiculo("ABC1234", cliente.id()));
            em.flush();
        }))
            .isExactlyInstanceOf(ConstraintViolationException.class)
            .satisfies(
                erro -> assertThat(((ConstraintViolationException) erro).getConstraintName())
                    .isEqualTo("uk_veiculos_placa")
            );
    }

    @Test
    void deveAceitarAnoSeguinteAoAtual() {
        emTransacao(em -> {
            final var veiculo = veiculo("ABC1234", cliente(em).id());
            final var ano = new Ano(Year.now(ZoneId.of("America/Sao_Paulo")).getValue() + 1);
            veiculo.setAno(ano);
            em.persist(veiculo);
            em.flush();
            em.clear();
            assertThat(em.find(Veiculo.class, veiculo.id()).ano()).isEqualTo(ano);
        });
    }

    @Test
    void deveRejeitarAnoSuperiorAoSeguinteNoBanco() {
        assertThatThrownBy(() -> emTransacao(em -> {
            final var veiculo = veiculo("ABC1234", cliente(em).id());
            em.persist(veiculo);
            em.flush();
            em
                .createNativeQuery("UPDATE oficina.veiculos SET ano = :ano WHERE id = :id")
                .setParameter("id", veiculo.id())
                .setParameter("ano", Year.now(ZoneId.of("America/Sao_Paulo")).getValue() + 2)
                .executeUpdate();
        }))
            .isExactlyInstanceOf(ConstraintViolationException.class)
            .satisfies(
                erro -> assertThat(((ConstraintViolationException) erro).getConstraintName())
                    .isEqualTo("veiculos_ano_check")
            );
    }

    @Test
    void deveRejeitarClienteSemCadastro() {
        assertThatThrownBy(() -> emTransacao(em -> {
            em.persist(veiculo("ABC1234", UUID.randomUUID()));
            em.flush();
        })).isExactlyInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void deveRejeitarHashIncompativel() {
        emTransacao(em -> {
            final var veiculo = veiculo("ABC1234", cliente(em).id());
            em.persist(veiculo);
            em.flush();
            em
                .createNativeQuery("UPDATE oficina.veiculos SET hash_placa = :hash WHERE id = :id")
                .setParameter("id", veiculo.id())
                .setParameter("hash", this.protecao.calcularHash(new Placa("DEF1G23")))
                .executeUpdate();
            em.clear();
            final var id = veiculo.id();
            assertThatThrownBy(() -> em.find(Veiculo.class, id))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("HMAC incompatível com a placa persistida");
        });
    }

    private static Cliente cliente(final EntityManager em) {
        final var cliente = new Cliente(new Nome("Maria"), new Email("maria@example.com"), new CPF("52998224725"));
        em.persist(cliente);
        return cliente;
    }

    private static Veiculo veiculo(final String placa, final UUID clienteId) {
        return new Veiculo(new Placa(placa), new Modelo("Corolla"), new Marca("Toyota"), new Ano(2024), clienteId);
    }

    private void emTransacao(final Consumer<EntityManager> acao) {
        try (final var em = this.fabrica.createEntityManager()) {
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
