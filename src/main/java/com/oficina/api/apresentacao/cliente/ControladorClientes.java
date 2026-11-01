package com.oficina.api.apresentacao.cliente;

import com.oficina.api.aplicacao.cliente.ServicoClientes;
import com.oficina.api.dominio.cliente.Documento;
import com.oficina.api.dominio.cliente.Email;
import com.oficina.api.dominio.cliente.Nome;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
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
@RequestMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Clientes", description = "Cadastro, consulta, atualização e exclusão de clientes")
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "400", description = "Requisição ou documento inválido", content = {
    @Content(schema = @Schema(implementation = ProblemDetail.class)) })
@ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = { @Content })
@ApiResponse(responseCode = "403", description = "Requer perfil ADMIN", content = { @Content })
public class ControladorClientes {
    private final ServicoClientes servico;

    public ControladorClientes(final ServicoClientes servico) {
        this.servico = servico;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Cadastrar cliente")
    @ApiResponse(responseCode = "201", description = "Cliente cadastrado")
    @ApiResponse(responseCode = "409", description = "Conflito com outro cliente já cadastrado", content = {
        @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    public ResponseEntity<RespostaCliente> criar(@Valid @RequestBody final RequisicaoCriarCliente requisicao) {
        final var cliente = this.servico
            .criar(new Nome(requisicao.nome()), new Email(requisicao.email()), Documento.de(requisicao.documento()));

        return ResponseEntity
            .created(URI.create("/clientes/" + cliente.id()))
            .cacheControl(CacheControl.noStore())
            .body(RespostaCliente.de(cliente));
    }

    @GetMapping(value = "/{id}")
    @Operation(summary = "Buscar cliente por ID")
    @ApiResponse(responseCode = "200", description = "Cliente encontrado")
    @ApiResponse(responseCode = "404", description = "Cliente inexistente ou excluído", content = {
        @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    public ResponseEntity<RespostaCliente> buscar(@PathVariable final UUID id) {
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noStore())
            .body(RespostaCliente.de(this.servico.buscar(id)));
    }

    @GetMapping
    @Operation(summary = "Buscar cliente por CPF ou CNPJ", description = "Informe documento apenas com dígitos.")
    @ApiResponse(responseCode = "200", description = "Cliente encontrado")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado", content = {
        @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    public ResponseEntity<RespostaCliente> buscarDocumento(@RequestParam final String documento) {
        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noStore())
            .body(RespostaCliente.de(this.servico.buscar(Documento.de(documento))));
    }

    @PatchMapping(value = "/{id}", consumes = { MediaType.APPLICATION_JSON_VALUE, "application/merge-patch+json" })
    @Operation(summary = "Atualizar cadastro do cliente", description = "Altera nome, email e/ou documento.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = {
            @Content(schema = @Schema(implementation = RequisicaoAtualizarCliente.class)) }))
    @ApiResponse(responseCode = "200", description = "Cliente atualizado")
    @ApiResponse(responseCode = "404", description = "Cliente inexistente ou excluído", content = {
        @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "409", description = "Conflito com outro cliente já cadastrado", content = {
        @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    public ResponseEntity<RespostaCliente> atualizar(
        @PathVariable final UUID id,
        @Valid @RequestBody final RequisicaoAtualizarCliente atualizacao
    ) {
        final var nome = atualizacao.nome();
        final var email = atualizacao.email();
        final var documento = atualizacao.documento();
        final var cliente = this.servico
            .atualizar(
                id,
                nome == null
                    ? null
                    : new Nome(nome),
                email == null
                    ? null
                    : new Email(email),
                documento == null
                    ? null
                    : Documento.de(documento)
            );
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(RespostaCliente.de(cliente));
    }

    @DeleteMapping(value = "/{id}")
    @Operation(summary = "Excluir cliente")
    @ApiResponse(responseCode = "204", description = "Cliente excluído", content = { @Content })
    @ApiResponse(responseCode = "404", description = "Cliente inexistente ou já excluído", content = {
        @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    public ResponseEntity<Void> excluir(@PathVariable final UUID id) {
        this.servico.excluir(id);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
