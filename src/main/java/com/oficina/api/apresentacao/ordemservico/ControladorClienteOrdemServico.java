package com.oficina.api.apresentacao.ordemservico;

import com.oficina.api.aplicacao.ordemservico.ServicoOrdensServico;
import com.oficina.api.apresentacao.RespostasHttp;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControladorClienteOrdemServico {
    private final ServicoOrdensServico servico;

    public ControladorClienteOrdemServico(final ServicoOrdensServico servico) {
        this.servico = servico;
    }

    @GetMapping(value = "/ordens-servico/decisao/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consultar orçamento com o token de decisão")
    public ResponseEntity<RespostaAcompanhamento> consultar(@PathVariable final String token) {
        return RespostasHttp.ok(RespostaAcompanhamento.de(this.servico.consultarDecisao(token)));
    }

    @Hidden
    @GetMapping(value = { "/cliente/{token}/status", "/cliente/{token}/aprovacao", "/cliente/{token}/rejeicao" },
        produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> pagina(@PathVariable final String token) throws IOException {
        // Valida somente a sintaxe; a API valida autorização e validade ao executar a
        // consulta.
        if (!token.matches("[A-Za-z0-9_-]{43}")) {
            return ResponseEntity.notFound().build();
        }
        final var recurso = new ClassPathResource("paginas/cliente.html");
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noStore())
            .body(Objects.requireNonNull(recurso.getContentAsString(StandardCharsets.UTF_8)));
    }
}
