package com.oficina.api.steps;

import com.oficina.api.apresentacao.auth.RequisicaoLogin;
import com.oficina.api.apresentacao.auth.RespostaLogin;
import com.oficina.api.apresentacao.auth.TipoToken;
import com.oficina.api.infraestrutura.seguranca.EmissorTokenJwt;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import static org.assertj.core.api.Assertions.assertThat;

public class LoginSteps {
    @Autowired
    private HttpSteps http;

    @Autowired
    private UserDetailsService usuarios;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmissorTokenJwt emissor;

    private @Nullable RequisicaoLogin credenciaisValidas;

    @Given("que existe um administrador cadastrado")
    public void cadastrarAdministrador() {
        final var credenciais = new RequisicaoLogin("administrador-" + UUID.randomUUID(), UUID.randomUUID().toString());
        final var administrador = User
            .withUsername(credenciais.nomeUsuario())
            .password(this.passwordEncoder.encode(credenciais.senha()))
            .roles("ADMIN")
            .build();
        ((UserDetailsManager) this.usuarios).createUser(administrador);
        this.credenciaisValidas = credenciais;
    }

    @When("acesso uma rota protegida com token inválido")
    public void acessarComTokenInvalido() {
        this.http.receber(this.http.requisicao().auth().oauth2("invalido").get("/protegida")); // NOSONAR S6437
    }

    @When("acesso uma rota protegida com token sem perfil administrador")
    public void acessarSemPerfil() {
        final var tokenSemPerfil = this.emissor.emitir("cliente", Set.of("CLIENTE"));
        this.http.receber(this.http.requisicao().auth().oauth2(tokenSemPerfil.valor()).get("/protegida"));
    }

    @When("acesso uma rota protegida com token válido apenas no cookie")
    public void acessarComTokenNoCookie() {
        final var tokenAdmin = this.emissor.emitir("admin", Set.of("ADMIN"));
        this.http.receber(this.http.requisicao().cookie("access_token", tokenAdmin.valor()).get("/protegida"));
    }

    @When("acesso uma rota protegida sem token")
    public void acessarSemToken() {
        this.http.receber(this.http.requisicao().get("/protegida"));
    }

    @When("envio as credenciais {string} e {string}")
    public void enviarCredenciais(final String usuario, final String senha) {
        this.http.receber(this.http.requisicao().body(new RequisicaoLogin(usuario, senha)).post("/login"));
    }

    @When("envio suas credenciais válidas")
    public void enviarCredenciaisValidas() {
        final var credenciais = Objects.requireNonNull(this.credenciaisValidas);
        enviarCredenciais(credenciais.nomeUsuario(), credenciais.senha());
    }

    @Then("devo acessar uma rota protegida com o token")
    public void acessarComToken() {
        final var respostaProtegida = this.http.requisicao().get("/protegida");
        assertThat(respostaProtegida.statusCode()).isEqualTo(404);
    }

    @Then("devo receber um token válido de administrador")
    public void validarToken() {
        assertThat(this.http.resposta().statusCode()).isEqualTo(200);
        assertThat(this.http.resposta().contentType()).startsWith("application/json");
        final var login = this.http.resposta().as(RespostaLogin.class);
        assertThat(login.tipoToken()).isEqualTo(TipoToken.BEARER);
        this.http.autenticar(login.tokenAcesso());
    }
}
