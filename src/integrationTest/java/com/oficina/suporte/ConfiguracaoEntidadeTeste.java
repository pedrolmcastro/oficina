package com.oficina.suporte;

import com.oficina.api.dominio.Entidade;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

// Fora de com.oficina.api para não entrar no mapeamento JPA da aplicação.
@Configuration(proxyBeanMethods = false)
@AutoConfigurationPackage
public class ConfiguracaoEntidadeTeste {
    @Entity
    @Table(name = "entidades_teste")
    public static class EntidadeTeste extends Entidade {}
}
