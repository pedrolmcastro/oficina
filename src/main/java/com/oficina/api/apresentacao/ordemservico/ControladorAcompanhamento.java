package com.oficina.api.apresentacao.ordemservico;

import com.oficina.api.aplicacao.ordemservico.ServicoOrdensServico;
import com.oficina.api.apresentacao.RespostasHttp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/ordens-servico/acompanhamento/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Acompanhamento do cliente", description = "O token secreto da URL autoriza acesso somente à sua OS. "
    + "Não requer conta ou JWT administrativo. Não registrar nem compartilhar a URL.")
@ApiResponse(responseCode = "404", description = "Acesso inválido, expirado ou substituído")
@ApiResponse(responseCode = "409", description = "OS não aguarda confirmação ou já recebeu decisão oposta")
public class ControladorAcompanhamento {
    private final ServicoOrdensServico servico;

    public ControladorAcompanhamento(final ServicoOrdensServico servico) {
        this.servico = servico;
    }

    @GetMapping
    @Operation(summary = "Consultar andamento e orçamento da OS",
        description = "Retorna apenas status, resultado, nomes, quantidades e valores do orçamento.")
    public ResponseEntity<RespostaAcompanhamento> acompanhar(@PathVariable final String token) {
        return RespostasHttp.ok(RespostaAcompanhamento.de(this.servico.acompanhar(token)));
    }

    @PostMapping(value = "/aprovacao")
    @Operation(summary = "Aprovar orçamento e liberar execução",
        description = "Utiliza o token de decisão enviado por e-mail, distinto do token de acompanhamento.")
    public ResponseEntity<RespostaAcompanhamento> aprovar(@PathVariable final String token) {
        return RespostasHttp.ok(RespostaAcompanhamento.de(this.servico.decidir(token, true)));
    }

    @PostMapping(value = "/rejeicao")
    @Operation(summary = "Rejeitar orçamento e aguardar retirada do veículo",
        description = "Utiliza o token de decisão enviado por e-mail, distinto do token de acompanhamento.")
    public ResponseEntity<RespostaAcompanhamento> rejeitar(@PathVariable final String token) {
        return RespostasHttp.ok(RespostaAcompanhamento.de(this.servico.decidir(token, false)));
    }
}
