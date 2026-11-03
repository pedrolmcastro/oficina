package com.oficina.api.apresentacao.insumo;

import com.oficina.api.aplicacao.Pagina;
import com.oficina.api.aplicacao.insumo.ServicoInsumos;
import com.oficina.api.apresentacao.RespostaLista;
import com.oficina.api.apresentacao.RespostasHttp;
import com.oficina.api.dominio.Preco;
import com.oficina.api.dominio.Quantidade;
import com.oficina.api.dominio.TextoNormalizado;
import com.oficina.api.dominio.insumo.Nome;
import com.oficina.api.dominio.insumo.PrecoVenda;
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
@RequestMapping(value = "/insumos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Insumos", description = "Gestão de insumos")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "400", description = "Requisição inválida", content = {
    @Content(schema = @Schema(implementation = ProblemDetail.class)) })
@ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = { @Content })
@ApiResponse(responseCode = "403", description = "Acesso negado", content = { @Content })
public class ControladorInsumos {
    private final ServicoInsumos servico;

    public ControladorInsumos(final ServicoInsumos servico) {
        this.servico = servico;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cadastrar insumo")
    @ApiResponse(responseCode = "201", description = "Insumo cadastrado")
    @ApiResponse(responseCode = "409", description = "Conflito com outro insumo já cadastrado")
    public ResponseEntity<RespostaInsumo> criar(@Valid @RequestBody final RequisicaoCriarInsumo requisicao) {
        final var insumo = this.servico
            .criar(
                new Nome(new TextoNormalizado(requisicao.nome())),
                new PrecoVenda(new Preco(requisicao.precoVenda()))
            );
        return RespostasHttp.criada(URI.create("/insumos/" + insumo.id()), RespostaInsumo.de(insumo));
    }

    @GetMapping(value = "/{id}")
    @Operation(summary = "Buscar insumo por ID")
    @ApiResponse(responseCode = "404", description = "Insumo inexistente ou excluído")
    public ResponseEntity<RespostaInsumo> buscar(@PathVariable final UUID id) {
        return RespostasHttp.ok(RespostaInsumo.de(this.servico.buscar(id)));
    }

    @GetMapping
    @Operation(summary = "Listar insumos", description = "Busca opcional por nome em português. Ordenados por ID")
    public ResponseEntity<RespostaLista<RespostaInsumo>> listar(
        @RequestParam(required = false) final @Nullable String nome,
        @RequestParam(required = false) final @Nullable UUID after,
        @RequestParam(defaultValue = "25") @Schema(minimum = "1", maximum = "25") final int first
    ) {
        return RespostasHttp
            .ok(RespostaLista.de(this.servico.listar(nome, after, new Pagina.Limite(first)), RespostaInsumo::de));
    }

    @PatchMapping(value = "/{id}", consumes = { MediaType.APPLICATION_JSON_VALUE, "application/merge-patch+json" })
    @Operation(summary = "Atualizar insumo")
    @ApiResponse(responseCode = "404", description = "Insumo inexistente ou excluído")
    @ApiResponse(responseCode = "409", description = "Conflito com outro insumo já cadastrado")
    public ResponseEntity<RespostaInsumo> atualizar(
        @PathVariable final UUID id,
        @Valid @RequestBody final RequisicaoAtualizarInsumo atualizacao
    ) {
        final var nome = atualizacao.nome();
        final var precoVenda = atualizacao.precoVenda();
        final var insumo = this.servico
            .atualizar(
                id,
                nome == null
                    ? null
                    : new Nome(new TextoNormalizado(nome)),
                precoVenda == null
                    ? null
                    : new PrecoVenda(new Preco(precoVenda))
            );
        return RespostasHttp.ok(RespostaInsumo.de(insumo));
    }

    @PostMapping(value = "/{id}/reposicoes-estoque", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Repor estoque do insumo",
        description = "Quantidade adicionada ao saldo físico. Reservas para ordens de serviço feitas automaticamente.")
    @ApiResponse(responseCode = "200", description = "Estoque reposto e fila de reservas processada")
    @ApiResponse(responseCode = "404", description = "Insumo inexistente ou excluído")
    public ResponseEntity<RespostaInsumo> reporEstoque(
        @PathVariable final UUID id,
        @Valid @RequestBody final RequisicaoReposicaoEstoque requisicao
    ) {
        return RespostasHttp
            .ok(RespostaInsumo.de(this.servico.reporEstoque(id, new Quantidade(requisicao.quantidade()))));
    }

    @DeleteMapping(value = "/{id}")
    @Operation(summary = "Excluir insumo")
    @ApiResponse(responseCode = "204", description = "Insumo excluído", content = { @Content })
    @ApiResponse(responseCode = "409",
        description = "Insumo obrigatório de serviço ativo ou necessário para ordem de serviço ativa")
    @ApiResponse(responseCode = "404", description = "Insumo inexistente ou já excluído")
    public ResponseEntity<Void> excluir(@PathVariable final UUID id) {
        this.servico.excluir(id);
        return RespostasHttp.semConteudo();
    }
}
