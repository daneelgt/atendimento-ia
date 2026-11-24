package br.com.atendimento.integration;

import br.com.atendimento.ai.ClienteIA;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes de integração: PostgreSQL real em container (um para toda a suíte),
 * IA substituída por mock e worker agendado desligado (os testes disparam o ciclo manualmente).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @MockitoBean
    protected ClienteIA clienteIA;

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void limparBanco() {
        jdbc.execute("TRUNCATE historico_atendimento, atendimento, avaliacao_execucao RESTART IDENTITY CASCADE");
    }

    protected static String json(String categoria, String prioridade, String assunto, double confianca) {
        return """
                {"categoria":"%s","prioridade":"%s","assunto":"%s","confianca":%s,
                 "resposta_sugerida":"Olá! Recebemos sua mensagem e o setor responsável vai analisar."}"""
                .formatted(categoria, prioridade, assunto, confianca);
    }
}
