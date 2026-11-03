package com.oficina.api.infraestrutura.documentacao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public final class SwaggerAutorizacao extends SwaggerIndexPageTransformer {
    private final String interceptadores;

    public SwaggerAutorizacao(
        final SwaggerUiConfigProperties configuracao,
        final SwaggerUiOAuthProperties oauth,
        final SwaggerWelcomeCommon welcome,
        final ObjectMapperProvider mapper
    ) throws IOException {
        super(configuracao, oauth, welcome, mapper);
        this.interceptadores = new ClassPathResource("documentacao/swagger-autorizacao.js")
            .getContentAsString(StandardCharsets.UTF_8);
    }

    @Override
    protected String defaultTransformations(final SwaggerUiConfigParameters parametros, final InputStream conteudo)
        throws IOException {
        return super.defaultTransformations(parametros, conteudo)
            .replace("presets: [", "..." + this.interceptadores + ",\npresets: [");
    }
}
