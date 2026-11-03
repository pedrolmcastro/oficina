package com.oficina.api.apresentacao.ordemservico;

import com.oficina.api.aplicacao.Pagina;
import com.oficina.api.aplicacao.ordemservico.ServicoOrdensServico;
import com.oficina.api.apresentacao.RespostaLista;
import com.oficina.api.apresentacao.RespostasHttp;
import com.oficina.api.dominio.DadoInvalidoException;
import com.oficina.api.dominio.Quantidade;
import com.oficina.api.dominio.ordemservico.StatusOrdemServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.Arrays;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
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
@RequestMapping(value = "/ordens-servico", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Ordens de serviço")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "400", description = "Requisição inválida")
@ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
@ApiResponse(responseCode = "403", description = "Acesso negado")
@ApiResponse(responseCode = "409",
    description = "Operação incompatível com o status ou insumo necessário da ordem de serviço")
public class ControladorOrdensServico {
    private final ServicoOrdensServico servico;

    public ControladorOrdensServico(final ServicoOrdensServico servico) {
        this.servico = servico;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cadastra ordem de serviço")
    @ApiResponse(responseCode = "201", description = "Ordem de serviço cadastrada")
    @ApiResponse(responseCode = "404", description = "Veículo, cliente, serviço ou insumo obrigatório não encontrado")
    public ResponseEntity<RespostaAcessoOrdemServico> criar(
        @Valid @RequestBody final RequisicaoCriarOrdemServico requisicao,
        final Principal autor
    ) {
        final var acesso = this.servico.criar(requisicao.veiculoId(), requisicao.servicoIds(), autor.getName());
        return RespostasHttp
            .criada(URI.create("/ordens-servico/" + acesso.ordemServico().id()), RespostaAcessoOrdemServico.de(acesso));
    }

    @GetMapping(value = "/{id}")
    @Operation(summary = "Consultar OS com orçamento e histórico")
    @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada")
    public ResponseEntity<RespostaOrdemServico> buscar(@PathVariable final UUID id) {
        return RespostasHttp.ok(RespostaOrdemServico.de(this.servico.buscar(id)));
    }

    @GetMapping
    @Operation(summary = "Listar ordem de serviços por status",
        description = "Informe o status em minúsculas com hífens. Ordenadas por ID")
    public ResponseEntity<RespostaLista<RespostaOrdemServico>> listar(
        @RequestParam(required = false) final @Nullable String status,
        @RequestParam(required = false) final @Nullable UUID after,
        @RequestParam(defaultValue = "25") final int first
    ) {
        final var statusFiltrado = status == null
            ? null
            : Arrays
                .stream(StatusOrdemServico.values())
                .filter(e -> e.valor().equals(status))
                .findFirst()
                .orElseThrow(() -> new DadoInvalidoException("Status de OS inválido"));
        return RespostasHttp
            .ok(
                RespostaLista
                    .de(this.servico.listar(statusFiltrado, after, new Pagina.Limite(first)), RespostaOrdemServico::de)
            );
    }

    @GetMapping(value = "/{id}/insumos-necessarios")
    @Operation(summary = "Listar insumos necessários da OS, mantendo suas origens separadas")
    @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada")
    public ResponseEntity<RespostaLista<RespostaInsumoNecessario>> listarInsumosNecessarios(
        @PathVariable final UUID id,
        @RequestParam(required = false) final @Nullable UUID after,
        @RequestParam(defaultValue = "25") final int first
    ) {
        final var insumosNecessarios = this.servico
            .buscar(id)
            .insumosNecessarios()
            .stream()
            .filter(i -> after == null || i.id().toString().compareTo(after.toString()) > 0)
            .sorted(java.util.Comparator.comparing(i -> i.id().toString()))
            .limit(new Pagina.Limite(first).quantidadeParaConsulta())
            .toList();
        return RespostasHttp
            .ok(
                RespostaLista.de(Pagina.de(insumosNecessarios, new Pagina.Limite(first)), RespostaInsumoNecessario::de)
            );
    }

    @PostMapping(value = "/{id}/insumos-necessarios", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Incluir insumo avulso na ordem de serviço")
    @ApiResponse(responseCode = "201", description = "Insumo incluído")
    @ApiResponse(responseCode = "404", description = "Ordem de serviço ou insumo não encontrado")
    public ResponseEntity<RespostaInsumoNecessario> adicionarInsumoAvulso(
        @PathVariable final UUID id,
        @Valid @RequestBody final RequisicaoCriarInsumoAvulso requisicao,
        final Principal autor
    ) {
        final var insumoNecessario = this.servico
            .adicionarInsumoAvulso(id, requisicao.insumoId(), new Quantidade(requisicao.quantidade()), autor.getName());
        return RespostasHttp
            .criada(
                URI.create("/ordens-servico/" + id + "/insumos-necessarios/" + insumoNecessario.id()),
                RespostaInsumoNecessario.de(insumoNecessario)
            );
    }

    @PatchMapping(value = "/{id}/insumos-necessarios/{insumoNecessarioId}", consumes = {
        MediaType.APPLICATION_JSON_VALUE,
        "application/merge-patch+json" })
    @Operation(summary = "Atualizar quantidade do insumo avulso necessário")
    @ApiResponse(responseCode = "404", description = "Ordem de serviço ou insumo necessário não encontrado")
    public ResponseEntity<RespostaInsumoNecessario> alterarQuantidadeInsumoAvulso(
        @PathVariable final UUID id,
        @PathVariable final UUID insumoNecessarioId,
        @Valid @RequestBody final RequisicaoAtualizarInsumoAvulso requisicao,
        final Principal autor
    ) {
        return RespostasHttp
            .ok(
                RespostaInsumoNecessario
                    .de(
                        this.servico
                            .alterarQuantidadeInsumoAvulso(
                                id,
                                insumoNecessarioId,
                                new Quantidade(requisicao.quantidade()),
                                autor.getName()
                            )
                    )
            );
    }

    @DeleteMapping(value = "/{id}/insumos-necessarios/{insumoNecessarioId}")
    @Operation(summary = "Remover insumo avulso da ordem de serviço")
    @ApiResponse(responseCode = "404", description = "Ordem de serviço ou insumo necessário não encontrado")
    public ResponseEntity<Void> removerInsumoAvulso(
        @PathVariable final UUID id,
        @PathVariable final UUID insumoNecessarioId,
        final Principal autor
    ) {
        this.servico.executar(id, "remover_insumo", o -> o.removerInsumoAvulso(insumoNecessarioId, autor.getName()));
        return RespostasHttp.semConteudo();
    }

    @PostMapping(value = "/{id}/acesso-cliente")
    @Operation(summary = "Reemitir acesso do cliente, invalidando imediatamente o token anterior")
    @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada")
    public ResponseEntity<RespostaAcessoOrdemServico> renovarAcesso(
        @PathVariable final UUID id,
        final Principal autor
    ) {
        return RespostasHttp.ok(RespostaAcessoOrdemServico.de(this.servico.renovarAcesso(id, autor.getName())));
    }

    @PostMapping(value = "/{id}/inicio-diagnostico")
    @Operation(summary = "Iniciar diagnóstico da ordem de serviço")
    @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada")
    public ResponseEntity<RespostaOrdemServico> iniciarDiagnostico(@PathVariable final UUID id, final Principal autor) {
        return RespostasHttp
            .ok(
                RespostaOrdemServico
                    .de(this.servico.executar(id, "iniciarDiagnostico", o -> o.iniciarDiagnostico(autor.getName())))
            );
    }

    @PostMapping(value = "/{id}/fim-diagnostico")
    @Operation(summary = "Finalizar diagnóstico da ordem de serviço")
    @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada")
    public ResponseEntity<RespostaOrdemServico> concluirDiagnostico(
        @PathVariable final UUID id,
        final Principal autor
    ) {
        return RespostasHttp
            .ok(
                RespostaOrdemServico
                    .de(this.servico.executar(id, "concluirDiagnostico", o -> o.concluirDiagnostico(autor.getName())))
            );
    }

    @PostMapping(value = "/{id}/inicio-execucao")
    @Operation(summary = "Iniciar execução da ordem de serviço")
    @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada")
    public ResponseEntity<RespostaOrdemServico> iniciarExecucao(@PathVariable final UUID id, final Principal autor) {
        return RespostasHttp
            .ok(
                RespostaOrdemServico
                    .de(this.servico.executar(id, "iniciarExecucao", o -> o.iniciarExecucao(autor.getName())))
            );
    }

    @PostMapping(value = "/{id}/fim-execucao")
    @Operation(summary = "Finalizar execução da ordem de serviço")
    @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada")
    public ResponseEntity<RespostaOrdemServico> concluirExecucao(@PathVariable final UUID id, final Principal autor) {
        return RespostasHttp
            .ok(
                RespostaOrdemServico
                    .de(this.servico.executar(id, "concluirExecucao", o -> o.concluirExecucao(autor.getName())))
            );
    }

    @PostMapping(value = "/{id}/retirada-veiculo")
    @Operation(summary = "Cadastrar retirada do veículo")
    @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada")
    public ResponseEntity<RespostaOrdemServico> retirarVeiculo(@PathVariable final UUID id, final Principal autor) {
        return RespostasHttp
            .ok(
                RespostaOrdemServico
                    .de(this.servico.executar(id, "retirarVeiculo", o -> o.retirarVeiculo(autor.getName())))
            );
    }
}
