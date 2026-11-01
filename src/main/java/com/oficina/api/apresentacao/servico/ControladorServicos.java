package com.oficina.api.apresentacao.servico;

import com.oficina.api.aplicacao.servico.ServicoServicos;
import com.oficina.api.dominio.servico.Nome;
import com.oficina.api.dominio.servico.PrecoVenda;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.CacheControl;
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
@RequestMapping(value = "/servicos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Serviços", description = "Cadastro, consulta, atualização e exclusão de serviços")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "400", description = "Requisição inválida", content = {
    @Content(schema = @Schema(implementation = ProblemDetail.class)) })
@ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = { @Content })
@ApiResponse(responseCode = "403", description = "Requer perfil ADMIN", content = { @Content })
public class ControladorServicos {
    private final ServicoServicos servico;

    public ControladorServicos(final ServicoServicos servico) {
        this.servico = servico;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cadastrar serviço")
    @ApiResponse(responseCode = "201", description = "Serviço cadastrado")
    @ApiResponse(responseCode = "409", description = "Nome já cadastrado")
    public ResponseEntity<RespostaServico> criar(@Valid @RequestBody final RequisicaoCriarServico requisicao) {
        final var servico = this.servico
            .criar(
                new Nome(requisicao.nome()),
                new PrecoVenda(requisicao.precoVenda()),
                requisicao.requerDiagnostico()
            );
        return ResponseEntity
            .created(URI.create("/servicos/" + servico.id()))
            .cacheControl(CacheControl.noStore())
            .body(RespostaServico.de(servico));
    }

    @GetMapping(value = "/{id}")
    @Operation(summary = "Buscar serviço por ID")
    @ApiResponse(responseCode = "404", description = "Serviço inexistente ou excluído")
    public ResponseEntity<RespostaServico> buscar(@PathVariable final UUID id) {
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noStore())
            .body(RespostaServico.de(this.servico.buscar(id)));
    }

    @GetMapping
    @Operation(summary = "Listar serviços", description = "Busca FULLTEXT opcional por nome em português. "
        + "Retorna até first serviços (padrão 25, entre 1 e 25), em ordem crescente de ID, após after. "
        + "Use nextCursor em after com o mesmo filtro; null indica o fim da listagem.")
    public ResponseEntity<RespostaListaServicos> listar(
        @RequestParam(required = false) final @Nullable String nome,
        @RequestParam(required = false) final @Nullable UUID after,
        @RequestParam(defaultValue = "25") @Min(value = 1) @Max(value = 25) final int first
    ) {
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noStore())
            .body(RespostaListaServicos.de(this.servico.listar(nome, after, first)));
    }

    @PatchMapping(value = "/{id}", consumes = { MediaType.APPLICATION_JSON_VALUE, "application/merge-patch+json" })
    @Operation(summary = "Atualizar serviço")
    @ApiResponse(responseCode = "404", description = "Serviço inexistente ou excluído")
    @ApiResponse(responseCode = "409", description = "Nome já cadastrado")
    public ResponseEntity<RespostaServico> atualizar(
        @PathVariable final UUID id,
        @Valid @RequestBody final RequisicaoAtualizarServico atualizacao
    ) {
        final var nome = atualizacao.nome();
        final var precoVenda = atualizacao.precoVenda();
        final var servico = this.servico
            .atualizar(
                id,
                nome == null
                    ? null
                    : new Nome(nome),
                precoVenda == null
                    ? null
                    : new PrecoVenda(precoVenda),
                atualizacao.requerDiagnostico()
            );
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(RespostaServico.de(servico));
    }

    @DeleteMapping(value = "/{id}")
    @Operation(summary = "Excluir serviço")
    @ApiResponse(responseCode = "204", description = "Serviço excluído", content = { @Content })
    @ApiResponse(responseCode = "404", description = "Serviço inexistente ou já excluído")
    public ResponseEntity<Void> excluir(@PathVariable final UUID id) {
        this.servico.excluir(id);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
