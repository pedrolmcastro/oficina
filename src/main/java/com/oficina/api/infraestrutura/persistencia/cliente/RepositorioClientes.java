package com.oficina.api.infraestrutura.persistencia.cliente;

import com.oficina.api.dominio.cliente.Cliente;
import com.oficina.api.dominio.cliente.Documento;
import com.oficina.api.infraestrutura.persistencia.RepositorioEntidades;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioClientes extends RepositorioEntidades<Cliente> {
    private final CriptografiaDocumento criptografia;

    public RepositorioClientes(final EntityManager entityManager, final CriptografiaDocumento criptografia) {
        super(entityManager, Cliente.class);
        this.criptografia = criptografia;
    }

    public Optional<Cliente> buscar(final Documento documento) {
        final var cliente = this.entityManager
            .createQuery("FROM Cliente c WHERE c.documento.hash = :hash", Cliente.class)
            .setParameter("hash", this.criptografia.calcularHash(documento))
            .getSingleResultOrNull();

        return Optional.ofNullable(cliente);
    }
}
