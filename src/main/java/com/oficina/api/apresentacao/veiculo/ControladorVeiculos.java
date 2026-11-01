package com.oficina.api.apresentacao.veiculo;

import com.oficina.api.aplicacao.veiculo.ServicoVeiculos;
import com.oficina.api.dominio.veiculo.Ano;
import com.oficina.api.dominio.veiculo.Marca;
import com.oficina.api.dominio.veiculo.Modelo;
import com.oficina.api.dominio.veiculo.Placa;
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
@RequestMapping(value = "/veiculos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Veículos", description = "Cadastro, consulta, atualização e exclusão de veículos")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "400", description = "Requisição ou placa inválida", content = {
    @Content(schema = @Schema(implementation = ProblemDetail.class)) })
@ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = { @Content })
@ApiResponse(responseCode = "403", description = "Requer perfil ADMIN", content = { @Content })
public class ControladorVeiculos {
    private final ServicoVeiculos servico;

    public ControladorVeiculos(final ServicoVeiculos servico) {
        this.servico = servico;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cadastrar veículo")
    @ApiResponse(responseCode = "201", description = "Veículo cadastrado")
    @ApiResponse(responseCode = "404", description = "Cliente inexistente ou excluído", content = {
        @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "409", description = "Conflito com outro veículo já cadastrado", content = {
        @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    public ResponseEntity<RespostaVeiculo> criar(@Valid @RequestBody final RequisicaoCriarVeiculo requisicao) {
        final var veiculo = this.servico
            .criar(
                new Placa(requisicao.placa()),
                new Modelo(requisicao.modelo()),
                new Marca(requisicao.marca()),
                new Ano(requisicao.ano()),
                requisicao.clienteId()
            );

        return ResponseEntity
            .created(URI.create("/veiculos/" + veiculo.id()))
            .cacheControl(CacheControl.noStore())
            .body(RespostaVeiculo.de(veiculo));
    }

    @GetMapping(value = "/{id}")
    @Operation(summary = "Buscar veículo por ID")
    @ApiResponse(responseCode = "200", description = "Veículo encontrado")
    @ApiResponse(responseCode = "404", description = "Veículo inexistente ou excluído", content = {
        @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    public ResponseEntity<RespostaVeiculo> buscar(@PathVariable final UUID id) {
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noStore())
            .body(RespostaVeiculo.de(this.servico.buscar(id)));
    }

    @GetMapping
    @Operation(summary = "Listar veículos",
        description = "Informe ao menos um filtro: placa (antiga ou Mercosul, em maiúsculas e sem hífen) "
            + "ou ID do cliente. "
            + "Retorna até first veículos (padrão 25, entre 1 e 25), em ordem crescente de ID, após o ID after. "
            + "Use nextCursor em after para continuar com os mesmos filtros; null indica que não há mais páginas.")
    @ApiResponse(responseCode = "200", description = "Lista de veículos, vazia quando nenhum corresponde aos filtros")
    public ResponseEntity<RespostaListaVeiculos> listar(
        @RequestParam(required = false) final @Nullable String placa,
        @RequestParam(required = false) final @Nullable UUID cliente,
        @RequestParam(required = false) final @Nullable UUID after,
        @RequestParam(defaultValue = "25") @Min(value = 1) @Max(value = 25) final int first
    ) {
        final var filtroPlaca = placa == null
            ? null
            : new Placa(placa);
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noStore())
            .body(RespostaListaVeiculos.de(this.servico.listar(filtroPlaca, cliente, after, first)));
    }

    @PatchMapping(value = "/{id}", consumes = { MediaType.APPLICATION_JSON_VALUE, "application/merge-patch+json" })
    @Operation(summary = "Atualizar cadastro do veículo",
        description = "Altera placa, modelo, marca, ano e/ou clienteId.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = {
            @Content(schema = @Schema(implementation = RequisicaoAtualizarVeiculo.class)) }))
    @ApiResponse(responseCode = "200", description = "Veículo atualizado")
    @ApiResponse(responseCode = "404", description = "Veículo ou cliente inexistente ou excluído", content = {
        @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "409", description = "Conflito com outro veículo já cadastrado", content = {
        @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    public ResponseEntity<RespostaVeiculo> atualizar(
        @PathVariable final UUID id,
        @Valid @RequestBody final RequisicaoAtualizarVeiculo atualizacao
    ) {
        final var modelo = atualizacao.modelo();
        final var marca = atualizacao.marca();
        final var placa = atualizacao.placa();
        final var ano = atualizacao.ano();
        final var veiculo = this.servico
            .atualizar(
                id,
                modelo == null
                    ? null
                    : new Modelo(modelo),
                marca == null
                    ? null
                    : new Marca(marca),
                placa == null
                    ? null
                    : new Placa(placa),
                ano == null
                    ? null
                    : new Ano(ano),
                atualizacao.clienteId()
            );
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(RespostaVeiculo.de(veiculo));
    }

    @DeleteMapping(value = "/{id}")
    @Operation(summary = "Excluir veículo")
    @ApiResponse(responseCode = "204", description = "Veículo excluído", content = { @Content })
    @ApiResponse(responseCode = "404", description = "Veículo inexistente ou já excluído", content = {
        @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    public ResponseEntity<Void> excluir(@PathVariable final UUID id) {
        this.servico.excluir(id);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
