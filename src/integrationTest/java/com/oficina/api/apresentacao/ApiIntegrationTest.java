package com.oficina.api.apresentacao;

import com.oficina.api.ConfiguracaoIntegration;
import com.oficina.api.infraestrutura.seguranca.EmissorTokenJwt;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(value = ConfiguracaoIntegration.class)
abstract class ApiIntegrationTest {
    @LocalServerPort
    protected int porta;
    @Autowired
    protected EmissorTokenJwt emissor;
    @Autowired
    protected JdbcTemplate jdbc;

    protected RequestSpecification requisicao() {
        return RestAssured
            .given()
            .port(this.porta)
            .contentType("application/json")
            .auth()
            .oauth2(this.emissor.emitir("admin", Set.of("ADMIN")).valor());
    }

    protected static void validar(final Response resposta, final int status) {
        assertThat(resposta.statusCode()).as(resposta.body().asString()).isEqualTo(status);
        assertThat(resposta.header("Cache-Control")).isEqualTo("no-store");
    }
}
