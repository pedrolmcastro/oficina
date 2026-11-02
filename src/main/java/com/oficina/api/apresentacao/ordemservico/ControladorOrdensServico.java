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
@ApiResponse(responseCode = "401", description = "Token administrativo ausente ou inválido")
@ApiResponse(responseCode = "403", description = "Requer perfil ADMIN")
@ApiResponse(responseCode = "404", description = "Recurso não encontrado")
@ApiResponse(responseCode = "409", description = "Operação incompatível com o status ou item da OS")
public class ControladorOrdensServico {
    private final ServicoOrdensServico servico;

    public ControladorOrdensServico(final ServicoOrdensServico servico) {
        this.servico = servico;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Criar OS com serviços e insumos obrigatórios; retorna acesso do cliente")
    @ApiResponse(responseCode = "201", description = "OS criada")
    public ResponseEntity<RespostaAcessoOrdemServico> criar(
        @Valid @RequestBody final RequisicaoCriarOrdemServico requisicao,
        final Principal autor
    ) {
        final var acesso = this.servico
            .criar(requisicao.clienteId(), requisicao.veiculoId(), requisicao.servicoIds(), autor.getName());
        return RespostasHttp
            .criada(URI.create("/ordens-servico/" + acesso.ordemServico().id()), RespostaAcessoOrdemServico.de(acesso));
    }

    @GetMapping(value = "/{id}")
    @Operation(summary = "Consultar OS com orçamento e histórico")
    public ResponseEntity<RespostaOrdemServico> buscar(@PathVariable final UUID id) {
        return RespostasHttp.ok(RespostaOrdemServico.de(this.servico.buscar(id)));
    }

    @GetMapping
    @Operation(summary = "Listar OS por status", description = "Status em minúsculas separados por hífen. "
        + "Paginação crescente por ID com first (1 a 25, padrão 25) e after, mantendo o filtro status.")
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
    @Operation(summary = "Listar itens de insumos da OS, mantendo suas origens separadas")
    public ResponseEntity<RespostaLista<RespostaInsumoNecessario>> listarInsumos(
        @PathVariable final UUID id,
        @RequestParam(required = false) final @Nullable UUID after,
        @RequestParam(defaultValue = "25") final int first
    ) {
        final var itens = this.servico
            .buscar(id)
            .insumos()
            .stream()
            .filter(i -> after == null || i.id().toString().compareTo(after.toString()) > 0)
            .sorted(java.util.Comparator.comparing(i -> i.id().toString()))
            .limit(new Pagina.Limite(first).quantidadeParaConsulta())
            .toList();
        return RespostasHttp
            .ok(RespostaLista.de(Pagina.de(itens, new Pagina.Limite(first)), RespostaInsumoNecessario::de));
    }

    @PostMapping(value = "/{id}/insumos-necessarios", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Adicionar item avulso durante diagnóstico; quantidade inteira positiva")
    @ApiResponse(responseCode = "201", description = "Item incluído")
    public ResponseEntity<RespostaInsumoNecessario> adicionarInsumo(
        @PathVariable final UUID id,
        @Valid @RequestBody final RequisicaoCriarInsumoAvulso requisicao,
        final Principal autor
    ) {
        final var item = this.servico
            .adicionarInsumo(id, requisicao.insumoId(), new Quantidade(requisicao.quantidade()), autor.getName());
        return RespostasHttp
            .criada(
                URI.create("/ordens-servico/" + id + "/insumos-necessarios/" + item.id()),
                RespostaInsumoNecessario.de(item)
            );
    }

    @PatchMapping(value = "/{id}/insumos-necessarios/{itemId}", consumes = {
        MediaType.APPLICATION_JSON_VALUE,
        "application/merge-patch+json" })
    @Operation(summary = "Substituir a quantidade absoluta de um item avulso durante diagnóstico")
    public ResponseEntity<RespostaInsumoNecessario> setQuantidade(
        @PathVariable final UUID id,
        @PathVariable final UUID itemId,
        @Valid @RequestBody final RequisicaoAtualizarInsumoAvulso requisicao,
        final Principal autor
    ) {
        return RespostasHttp
            .ok(
                RespostaInsumoNecessario
                    .de(
                        this.servico.setQuantidade(id, itemId, new Quantidade(requisicao.quantidade()), autor.getName())
                    )
            );
    }

    @DeleteMapping(value = "/{id}/insumos-necessarios/{itemId}")
    @Operation(summary = "Remover item avulso durante diagnóstico")
    public ResponseEntity<Void> removerInsumo(
        @PathVariable final UUID id,
        @PathVariable final UUID itemId,
        final Principal autor
    ) {
        this.servico.executar(id, "remover_insumo", o -> o.removerInsumo(itemId, autor.getName()));
        return RespostasHttp.semConteudo();
    }

    @PostMapping(value = "/{id}/acesso-cliente")
    @Operation(summary = "Reemitir acesso do cliente, invalidando imediatamente o token anterior")
    public ResponseEntity<RespostaAcessoOrdemServico> renovarAcesso(
        @PathVariable final UUID id,
        final Principal autor
    ) {
        return RespostasHttp.ok(RespostaAcessoOrdemServico.de(this.servico.renovarAcesso(id, autor.getName())));
    }

    @PostMapping(value = "/{id}/inicio-diagnostico")
    @Operation(summary = "Iniciar diagnóstico")
    public ResponseEntity<RespostaOrdemServico> iniciarDiagnostico(@PathVariable final UUID id, final Principal autor) {
        return RespostasHttp
            .ok(
                RespostaOrdemServico
                    .de(this.servico.executar(id, "iniciarDiagnostico", o -> o.iniciarDiagnostico(autor.getName())))
            );
    }

    @PostMapping(value = "/{id}/fim-diagnostico")
    @Operation(summary = "Concluir diagnóstico e disponibilizar orçamento")
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
    @Operation(summary = "Iniciar execução aprovada")
    public ResponseEntity<RespostaOrdemServico> iniciarExecucao(@PathVariable final UUID id, final Principal autor) {
        return RespostasHttp
            .ok(
                RespostaOrdemServico
                    .de(this.servico.executar(id, "iniciarExecucao", o -> o.iniciarExecucao(autor.getName())))
            );
    }

    @PostMapping(value = "/{id}/fim-execucao")
    @Operation(summary = "Concluir execução e aguardar retirada")
    public ResponseEntity<RespostaOrdemServico> concluirExecucao(@PathVariable final UUID id, final Principal autor) {
        return RespostasHttp
            .ok(
                RespostaOrdemServico
                    .de(this.servico.executar(id, "concluirExecucao", o -> o.concluirExecucao(autor.getName())))
            );
    }

    @PostMapping(value = "/{id}/retirada-veiculo")
    @Operation(summary = "Registrar retirada do veículo")
    public ResponseEntity<RespostaOrdemServico> retirarVeiculo(@PathVariable final UUID id, final Principal autor) {
        return RespostasHttp
            .ok(
                RespostaOrdemServico
                    .de(this.servico.executar(id, "retirarVeiculo", o -> o.retirarVeiculo(autor.getName())))
            );
    }
}
