package com.oficina.api.apresentacao.auth;

import com.oficina.api.infraestrutura.seguranca.EmissorTokenJwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.stream.Collectors;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Autenticação", description = "Emissão de tokens de acesso à API")
public class ControladorLogin {
    private final AuthenticationManager authenticationManager;
    private final EmissorTokenJwt emissorTokenJwt;

    public ControladorLogin(final AuthenticationManager authenticationManager, final EmissorTokenJwt emissorTokenJwt) {
        this.authenticationManager = authenticationManager;
        this.emissorTokenJwt = emissorTokenJwt;
    }

    @ExceptionHandler(value = AuthenticationException.class)
    public ResponseEntity<Void> authenticationExceptionHandler() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Autenticar usuário",
        description = "Autentica nome de usuário e senha e retorna um JWT para uso em Authorization: Bearer <token>."
    )
    @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso", content = {
        @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespostaLogin.class))
    })
    @ApiResponse(responseCode = "400", description = "JSON inválido", content = {
        @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(type = "object", description = "Erro de validação da requisição")
        )
    })
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas", content = {
        @Content
    })
    public ResponseEntity<RespostaLogin> login(@Valid @RequestBody final RequisicaoLogin requisicao) {
        final var credenciais = UsernamePasswordAuthenticationToken
            .unauthenticated(requisicao.nomeUsuario(), requisicao.senha());

        final var autenticacao = this.authenticationManager.authenticate(credenciais);

        final var perfis = autenticacao
            .getAuthorities()
            .stream()
            .map(permissao -> permissao.getAuthority())
            .filter(permissao -> permissao.startsWith("ROLE_"))
            .map(permissao -> permissao.substring("ROLE_".length()))
            .collect(Collectors.toUnmodifiableSet());

        final var token = this.emissorTokenJwt.emitir(autenticacao.getName(), perfis);

        return ResponseEntity
            .ok()
            .cacheControl(CacheControl.noStore())
            .body(new RespostaLogin(token.valor(), TipoToken.BEARER, token.validadeEmSegundos()));
    }
}
