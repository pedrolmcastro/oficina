package com.oficina.api;

import com.oficina.api.infraestrutura.email.EnvioEmails;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(value = ConfiguracaoBdd.class)
public class ConfiguracaoCucumber {
    @MockitoBean
    private EnvioEmails emails;
}
