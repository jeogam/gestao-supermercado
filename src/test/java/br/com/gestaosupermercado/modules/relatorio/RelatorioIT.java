package br.com.gestaosupermercado.modules.relatorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Relatórios contra um PostgreSQL real: totais só de vendas concluídas, custo e arredondamento,
 * PDF e permissões por perfil.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.security.jwt.secret=segredo-de-teste-com-mais-de-32-caracteres",
        "app.bootstrap.admin.email=admin@mercado.com",
        "app.bootstrap.admin.senha=AdminSenha1"
})
class RelatorioIT {

    private static final String SENHA_PADRAO = "Senha1234";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginAccessToken("admin@mercado.com", "AdminSenha1");
    }

    // Único teste que registra vendas, para os totais do período serem exatos.
    @Test
    void shouldReportOnlyConcluidasWithCustoAndRounding() throws Exception {
        final OffsetDateTime inicio = OffsetDateTime.now().minusSeconds(1);
        final Long arrozId = saveProdutoComEstoque("UN", "4.00", "10.00", "10");
        final Long queijoId = saveProdutoComEstoque("KG", "3.33", "39.90", "5");
        saveVenda("PIX", arrozId, "3");
        saveVenda("DINHEIRO", queijoId, "0.345");
        final long canceladaId = saveVenda("PIX", arrozId, "1");
        mockMvc.perform(put("/api/v1/venda/vendas/venda/{id}/cancelamento", canceladaId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        final OffsetDateTime fim = OffsetDateTime.now().plusMinutes(1);
        final String gerenteToken = newUsuarioToken("Gerente");

        // Arroz: 3 x 10,00 = 30,00, custo 3 x 4,00 = 12,00. Queijo: 0,345 x 39,90 = 13,77, custo 0,345 x 3,33 = 1,14885 -> 1,15.
        mockMvc.perform(relatorio("/faturamento", gerenteToken, inicio, fim))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeVendas").value(2))
                .andExpect(jsonPath("$.quantidadeVendasCanceladas").value(1))
                .andExpect(jsonPath("$.faturamento").value(43.77))
                .andExpect(jsonPath("$.custo").value(13.15))
                .andExpect(jsonPath("$.lucro").value(30.62))
                .andExpect(jsonPath("$.margemPercentual").value(69.96))
                .andExpect(jsonPath("$.formasPagamento.length()").value(2))
                .andExpect(jsonPath("$.formasPagamento[?(@.formaPagamento == 'PIX')].faturamento").value(Matchers.contains(30.0)));

        mockMvc.perform(relatorio("/margem", gerenteToken, inicio, fim))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].produtoId").value(arrozId))
                .andExpect(jsonPath("$[0].quantidadeVendida").value(3))
                .andExpect(jsonPath("$[0].lucro").value(18.00))
                .andExpect(jsonPath("$[0].margemPercentual").value(60.00))
                .andExpect(jsonPath("$[1].produtoId").value(queijoId))
                .andExpect(jsonPath("$[1].custo").value(1.15))
                .andExpect(jsonPath("$[1].lucro").value(12.62))
                .andExpect(jsonPath("$[1].margemPercentual").value(91.65));

        final byte[] pdf = mockMvc.perform(relatorio("/pdf", gerenteToken, inicio, fim))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, Matchers.startsWith("attachment")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void shouldAllowRelatoriosOnlyWithRelatorioVisualizarAndValidPeriodo() throws Exception {
        final OffsetDateTime agora = OffsetDateTime.now();

        mockMvc.perform(relatorio("/faturamento", newUsuarioToken("Caixa"), agora.minusDays(1), agora)).andExpect(status().isForbidden());
        mockMvc.perform(relatorio("/pdf", newUsuarioToken("Estoquista"), agora.minusDays(1), agora)).andExpect(status().isForbidden());
        mockMvc.perform(relatorio("/margem", adminToken, agora, agora.minusDays(1)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("A data inicial deve ser anterior ou igual à data final."));
        mockMvc.perform(get("/api/v1/relatorio/relatorios/pdf").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mockMvc.perform(relatorio("/faturamento", adminToken, agora.minusYears(10), agora.minusYears(9)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.faturamento").value(0))
                .andExpect(jsonPath("$.margemPercentual").value(0));
    }

    private MockHttpServletRequestBuilder relatorio(final String caminho, final String accessToken, final OffsetDateTime inicio,
                                                    final OffsetDateTime fim) {
        return get("/api/v1/relatorio/relatorios" + caminho)
                .param("dataInicio", inicio.toString())
                .param("dataFim", fim.toString())
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken));
    }

    private long saveVenda(final String formaPagamento, final Long produtoId, final String quantidade) throws Exception {
        final String json = mockMvc.perform(post("/api/v1/venda/vendas/venda")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"formaPagamento": "%s", "itens": [{"produtoId": %d, "quantidade": %s}]}
                                """.formatted(formaPagamento, produtoId, quantidade)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private Long saveProdutoComEstoque(final String unidadeMedida, final String precoCusto, final String precoVenda, final String estoque) throws Exception {
        final String json = mockMvc.perform(post("/api/v1/produto/produtos/produto")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Produto %s", "unidadeMedida": "%s", "precoCusto": %s, "precoVenda": %s, "estoqueMinimo": 0}
                                """.formatted(UUID.randomUUID(), unidadeMedida, precoCusto, precoVenda)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        final Long produtoId = ((Number) JsonPath.read(json, "$.id")).longValue();
        mockMvc.perform(post("/api/v1/estoque/movimentacoes/ajuste")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"produtoId": %d, "tipo": "ENTRADA", "quantidade": %s, "motivo": "Estoque inicial"}
                                """.formatted(produtoId, estoque)))
                .andExpect(status().isCreated());
        return produtoId;
    }

    private String newUsuarioToken(final String perfil) throws Exception {
        final String perfisJson = mockMvc.perform(get("/api/v1/core/perfis").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        final List<Number> perfilIds = JsonPath.read(perfisJson, "$[?(@.nome == '" + perfil + "')].id");
        final String email = "operador-" + UUID.randomUUID() + "@mercado.com";
        mockMvc.perform(post("/api/v1/core/usuarios/usuario")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Operador", "email": "%s", "senha": "%s", "perfilIds": [%d]}
                                """.formatted(email, SENHA_PADRAO, perfilIds.getFirst().longValue())))
                .andExpect(status().isCreated());
        return loginAccessToken(email, SENHA_PADRAO);
    }

    private String loginAccessToken(final String email, final String senha) throws Exception {
        final String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"senha\": \"%s\"}".formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.accessToken");
    }

    private static String bearer(final String accessToken) {
        return "Bearer " + accessToken;
    }
}
