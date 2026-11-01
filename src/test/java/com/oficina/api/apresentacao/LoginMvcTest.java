package com.oficina.api.apresentacao;

import com.oficina.api.apresentacao.auth.ControladorLogin;
import com.oficina.api.infraestrutura.seguranca.EmissorTokenJwt;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ControladorLogin.class)
@AutoConfigureMockMvc(addFilters = false)
class LoginMvcTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthenticationManager autenticador;

    @MockitoBean
    private EmissorTokenJwt emissor;

    @Test
    void deveEmitirTokenSomenteComPerfis() throws Exception {
        final var autenticacao = UsernamePasswordAuthenticationToken
            .authenticated("maria", "senha", AuthorityUtils.createAuthorityList("ROLE_ADMIN", "leitura"));
        when(this.autenticador.authenticate(any())).thenReturn(autenticacao);
        when(this.emissor.emitir("maria", Set.of("ADMIN"))).thenReturn(new EmissorTokenJwt.TokenEmitido("token", 1800));

        final var resposta = this.mvc
            .perform(
                post("/login")
                    .contentType("application/json")
                    .content("{\"nomeUsuario\":\"maria\",\"senha\":\"senha\"}")
            )
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(content().json("{\"tokenAcesso\":\"token\",\"tipoToken\":\"Bearer\",\"expiraEm\":1800}"))
            .andReturn()
            .getResponse();
        assertThat(resposta.getCookies()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{ \"nomeUsuario\": \"\", \"senha\": \"maria\" }",
        "{ \"nomeUsuario\": \"maria\", \"senha\":\"   \" }",
        "{ \"senha\": \"maria\" }",
        "{ \"nomeUsuario\": \"maria\" }",
        "{}",
        "{" })
    void deveRejeitarPayloadInvalido(final String json) throws Exception {
        this.mvc
            .perform(post("/login").contentType("application/json").content(json))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(this.autenticador, this.emissor);
    }
}
