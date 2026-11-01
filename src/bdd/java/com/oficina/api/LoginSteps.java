package com.oficina.api;

import com.oficina.api.apresentacao.auth.RequisicaoLogin;
import com.oficina.api.apresentacao.auth.RespostaLogin;
import com.oficina.api.infraestrutura.seguranca.EmissorTokenJwt;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.provisioning.UserDetailsManager;
import static org.assertj.core.api.Assertions.assertThat;

public class LoginSteps {
    @LocalServerPort
    private int porta;

    @Autowired
    private JwtDecoder decoder;

    @Autowired
    private UserDetailsService usuarios;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmissorTokenJwt emissor;

    private @Nullable Response resposta;
    private @Nullable String token;
    private @Nullable RequisicaoLogin credenciaisValidas;

    @Given("que existe um administrador cadastrado")
    public void cadastrarAdministrador() {
        final var credenciais = new RequisicaoLogin("administrador-" + UUID.randomUUID(), UUID.randomUUID().toString());
        final var administrador = User
            .withUsername(credenciais.nomeUsuario())
            .password(this.passwordEncoder.encode(credenciais.senha()))
            // Permissões comuns não devem ser publicadas como perfis no token.
            .authorities("ROLE_ADMIN", "leitura")
            .build();
        ((UserDetailsManager) this.usuarios).createUser(administrador);
        this.credenciaisValidas = credenciais;
    }

    @When("acesso uma rota protegida com token inválido")
    public void acessarComTokenInvalido() {
        this.resposta = requisicao().auth().oauth2("invalido").get("/protegida"); // NOSONAR S6437
    }

    @When("acesso uma rota protegida com token sem perfil administrador")
    public void acessarSemPerfil() {
        final var tokenSemPerfil = this.emissor.emitir("cliente", Set.of("CLIENTE"));
        this.resposta = requisicao().auth().oauth2(tokenSemPerfil.valor()).get("/protegida");
    }

    @When("acesso uma rota protegida com token válido apenas no cookie")
    public void acessarComTokenNoCookie() {
        final var tokenAdmin = this.emissor.emitir("admin", Set.of("ADMIN"));
        this.resposta = requisicao().cookie("access_token", tokenAdmin.valor()).get("/protegida");
    }

    @When("acesso uma rota protegida sem token")
    public void acessarSemToken() {
        this.resposta = requisicao().get("/protegida");
    }

    @When("envio as credenciais {string} e {string}")
    public void enviarCredenciais(final String usuario, final String senha) {
        this.resposta = requisicao().body(new RequisicaoLogin(usuario, senha)).post("/login");
    }

    @When("envio suas credenciais válidas")
    public void enviarCredenciaisValidas() {
        final var credenciais = Objects.requireNonNull(this.credenciaisValidas);
        enviarCredenciais(credenciais.nomeUsuario(), credenciais.senha());
    }

    @Then("a API deve retornar {int}")
    @Then("o login deve retornar {int}")
    public void validarStatus(final int status) {
        assertThat(resposta().statusCode()).isEqualTo(status);
    }

    @Then("a resposta não deve conter token nem permitir cache")
    public void validarRejeicao() {
        assertThat(resposta().body().asString()).isEmpty();
        assertThat(resposta().header("Cache-Control")).isEqualTo("no-store");
    }

    @Then("devo acessar uma rota protegida com o token")
    public void acessarComToken() {
        final var respostaProtegida = requisicao().auth().oauth2(Objects.requireNonNull(this.token)).get("/protegida");
        assertThat(respostaProtegida.statusCode()).isEqualTo(404);
    }

    @Then("devo receber um token válido de administrador")
    public void validarToken() {
        assertThat(resposta().statusCode()).isEqualTo(200);
        assertThat(resposta().contentType()).startsWith("application/json");
        assertThat(resposta().header("Cache-Control")).isEqualTo("no-store");
        assertThat(resposta().cookies()).isEmpty();
        final var login = resposta().as(RespostaLogin.class);
        assertThat(login.tipoToken()).isEqualTo("Bearer");
        assertThat(login.expiraEm()).isEqualTo(1800);
        this.token = login.tokenAcesso();
        final var jwt = this.decoder.decode(Objects.requireNonNull(this.token));
        assertThat(jwt.getSubject()).isEqualTo(Objects.requireNonNull(this.credenciaisValidas).nomeUsuario());
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
        assertThat(jwt.getIssuedAt()).isBetween(Instant.now().minusSeconds(30), Instant.now());
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).toSeconds()).isEqualTo(login.expiraEm());
    }

    private Response resposta() {
        return Objects.requireNonNull(this.resposta);
    }

    private RequestSpecification requisicao() {
        return RestAssured.given().port(this.porta).contentType("application/json");
    }
}
