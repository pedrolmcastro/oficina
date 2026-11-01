package com.oficina.api.infraestrutura.persistencia.cliente;

import com.oficina.api.dominio.cliente.Cliente;
import com.oficina.api.dominio.cliente.Documento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioClientes {
    private final EntityManager entityManager;
    private final ProtecaoDocumento protecao;

    public RepositorioClientes(final EntityManager entityManager, final ProtecaoDocumento protecao) {
        this.entityManager = entityManager;
        this.protecao = protecao;
    }

    public void adicionar(final Cliente cliente) {
        this.entityManager.persist(cliente);
    }

    public Optional<Cliente> buscar(final UUID id, final LockModeType modoBloqueio) {
        return Optional.ofNullable(this.entityManager.find(Cliente.class, id, modoBloqueio));
    }

    public Optional<Cliente> buscar(final Documento documento) {
        final var cliente = this.entityManager
            .createQuery("FROM Cliente c WHERE c.documento.hash = :hash", Cliente.class)
            .setParameter("hash", this.protecao.calcularHash(documento))
            .getSingleResultOrNull();

        return Optional.ofNullable(cliente);
    }
}
