package com.oficina.api.apresentacao.servico;

import com.oficina.api.aplicacao.Pagina;
import com.oficina.api.aplicacao.servico.ServicoServicos;
import com.oficina.api.apresentacao.RespostaLista;
import com.oficina.api.apresentacao.RespostasHttp;
import com.oficina.api.dominio.Quantidade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/servicos/{servicoId}/insumos-obrigatorios", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Serviços", description = "Gestão de insumos obrigatórios de serviços")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "400", description = "Requisição inválida", content = {
    @Content(schema = @Schema(implementation = ProblemDetail.class)) })
@ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = { @Content })
@ApiResponse(responseCode = "403", description = "Acesso negado", content = { @Content })
@ApiResponse(responseCode = "404", description = "Serviço ou insumo obrigatório não encontrado", content = {
    @Content(schema = @Schema(implementation = ProblemDetail.class)) })
public class ControladorInsumosObrigatorios {
    private final ServicoServicos servico;

    public ControladorInsumosObrigatorios(final ServicoServicos servico) {
        this.servico = servico;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Associar insumo obrigatório ao serviço")
    @ApiResponse(responseCode = "201", description = "Insumo obrigatório associado")
    @ApiResponse(responseCode = "409", description = "Insumo já é obrigatório para o serviço")
    public ResponseEntity<RespostaInsumoObrigatorio> associar(
        @PathVariable final UUID servicoId,
        @Valid @RequestBody final RequisicaoCriarInsumoObrigatorio requisicao
    ) {
        final var insumo = this.servico
            .associarInsumoObrigatorio(servicoId, requisicao.insumoId(), new Quantidade(requisicao.quantidade()));
        return RespostasHttp
            .criada(
                URI.create("/servicos/" + servicoId + "/insumos-obrigatorios/" + insumo.insumoId()),
                RespostaInsumoObrigatorio.de(insumo)
            );
    }

    @PatchMapping(value = "/{insumoId}", consumes = {
        MediaType.APPLICATION_JSON_VALUE,
        "application/merge-patch+json" })
    @Operation(summary = "Atualizar associação de insumo obrigatório")
    @ApiResponse(responseCode = "200", description = "Associação de insumo obrigatório atualizada")
    public ResponseEntity<RespostaInsumoObrigatorio> atualizar(
        @PathVariable final UUID servicoId,
        @PathVariable final UUID insumoId,
        @Valid @RequestBody final RequisicaoAtualizarInsumoObrigatorio requisicao
    ) {
        final var atualizado = this.servico
            .atualizarInsumoObrigatorio(servicoId, insumoId, new Quantidade(requisicao.quantidade()));
        return RespostasHttp.ok(RespostaInsumoObrigatorio.de(atualizado));
    }

    @GetMapping
    @Operation(summary = "Listar insumos obrigatórios do serviço", description = "Ordenados por ID.")
    public ResponseEntity<RespostaLista<RespostaInsumoObrigatorio>> listar(
        @PathVariable final UUID servicoId,
        @RequestParam(required = false) final @Nullable UUID after,
        @RequestParam(defaultValue = "25") @Schema(minimum = "1", maximum = "25") final int first
    ) {
        return RespostasHttp
            .ok(
                RespostaLista
                    .de(
                        this.servico.listarInsumosObrigatorios(servicoId, after, new Pagina.Limite(first)),
                        RespostaInsumoObrigatorio::de
                    )
            );
    }

    @DeleteMapping(value = "/{insumoId}")
    @Operation(summary = "Remover associação de insumo obrigatório")
    @ApiResponse(responseCode = "204", description = "Associação de insumo obrigatório removida", content = {
        @Content })
    public ResponseEntity<Void> remover(@PathVariable final UUID servicoId, @PathVariable final UUID insumoId) {
        this.servico.removerInsumoObrigatorio(servicoId, insumoId);
        return RespostasHttp.semConteudo();
    }
}
