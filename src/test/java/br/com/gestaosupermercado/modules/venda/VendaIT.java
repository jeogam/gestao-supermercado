package br.com.gestaosupermercado.modules.venda;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Fluxo ponta a ponta de vendas contra um PostgreSQL real: registro com baixa no estoque, troco, rollback quando falta
 * estoque, cancelamento com estorno e permissões por perfil.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.security.jwt.secret=segredo-de-teste-com-mais-de-32-caracteres",
        "app.bootstrap.admin.email=admin@mercado.com",
        "app.bootstrap.admin.senha=AdminSenha1"
})
class VendaIT {

    private static final String SENHA_PADRAO = "Senha1234";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;
    private String caixaToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginAccessToken("admin@mercado.com", "AdminSenha1");
        caixaToken = newUsuarioToken("Caixa");
    }

    @Test
    void shouldRegisterVendaGivingSaidaInEstoqueWithoutExposingCusto() throws Exception {
        final Long arrozId = saveProdutoComEstoque("UN", "23.90", "10");
        final Long queijoId = saveProdutoComEstoque("KG", "39.90", "2");

        final String vendaJson = saveVenda(caixaToken, "DINHEIRO", "100.00", """
                {"produtoId": %d, "quantidade": 2},
                {"produtoId": %d, "quantidade": 0.345}
                """.formatted(arrozId, queijoId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.valorTotal").value(61.57))
                .andExpect(jsonPath("$.troco").value(38.43))
                .andExpect(jsonPath("$.itens[0].precoUnitario").value(23.90))
                .andExpect(jsonPath("$.itens[0].precoCustoUnitario").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        final long vendaId = ((Number) JsonPath.read(vendaJson, "$.id")).longValue();

        assertEstoque(arrozId, "8");
        assertEstoque(queijoId, "1.655");
        mockMvc.perform(get("/api/v1/estoque/movimentacoes").param("produtoId", arrozId.toString()).param("origem", "VENDA")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.conteudo[0].tipo").value("SAIDA"))
                .andExpect(jsonPath("$.conteudo[0].referenciaId").value(vendaId));

        mockMvc.perform(get("/api/v1/venda/vendas/venda/{id}", vendaId).header(HttpHeaders.AUTHORIZATION, bearer(caixaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(2));
    }

    @Test
    void shouldRollbackWholeVendaWhenEstoqueInsuficiente() throws Exception {
        final Long comEstoqueId = saveProdutoComEstoque("UN", "5.00", "10");
        final Long semEstoqueId = saveProdutoComEstoque("UN", "5.00", "1");

        saveVenda(caixaToken, "PIX", null, """
                {"produtoId": %d, "quantidade": 3},
                {"produtoId": %d, "quantidade": 2}
                """.formatted(comEstoqueId, semEstoqueId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Estoque insuficiente para a operação."));

        assertEstoque(comEstoqueId, "10");
        assertEstoque(semEstoqueId, "1");
    }

    @Test
    void shouldRejectInvalidVendas() throws Exception {
        final Long produtoId = saveProdutoComEstoque("UN", "10.00", "10");

        saveVenda(caixaToken, "DINHEIRO", "5.00", """
                {"produtoId": %d, "quantidade": 1}
                """.formatted(produtoId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("O valor recebido é menor que o total da venda."));
        saveVenda(caixaToken, "PIX", null, """
                {"produtoId": %d, "quantidade": 1.5}
                """.formatted(produtoId))
                .andExpect(status().isUnprocessableContent());
        saveVenda(caixaToken, "PIX", null, """
                {"produtoId": %d, "quantidade": 1},
                {"produtoId": %d, "quantidade": 1}
                """.formatted(produtoId, produtoId))
                .andExpect(status().isUnprocessableContent());
        saveVenda(caixaToken, "PIX", null, "").andExpect(status().isUnprocessableContent());
        saveVenda(caixaToken, "CHEQUE", null, """
                {"produtoId": %d, "quantidade": 1}
                """.formatted(produtoId))
                .andExpect(status().isBadRequest());
        saveVenda(caixaToken, "PIX", null, """
                {"produtoId": 999999, "quantidade": 1}
                """)
                .andExpect(status().isNotFound());

        assertEstoque(produtoId, "10");
    }

    @Test
    void shouldCancelVendaReturningItensToEstoqueOnlyOnce() throws Exception {
        final Long produtoId = saveProdutoComEstoque("UN", "10.00", "10");
        final long vendaId = saveVendaId(produtoId, "4");
        assertEstoque(produtoId, "6");

        // Produto inativado depois da venda: o estorno devolve o estoque mesmo assim.
        mockMvc.perform(put("/api/v1/produto/produtos/produto/{id}/status", produtoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\": false}"))
                .andExpect(status().isNoContent());

        cancelVenda(caixaToken, vendaId, "Cliente desistiu")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.motivoCancelamento").value("Cliente desistiu"))
                .andExpect(jsonPath("$.canceladoEm").exists());
        assertEstoque(produtoId, "10");

        cancelVenda(caixaToken, vendaId, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("A venda já está cancelada."));
        assertEstoque(produtoId, "10");

        mockMvc.perform(get("/api/v1/estoque/movimentacoes").param("produtoId", produtoId.toString()).param("origem", "ESTORNO_VENDA")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.conteudo[0].referenciaId").value(vendaId));

        mockMvc.perform(get("/api/v1/venda/vendas").param("status", "CANCELADA")
                        .header(HttpHeaders.AUTHORIZATION, bearer(caixaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[?(@.id == " + vendaId + ")]").exists());

        cancelVenda(caixaToken, 999_999L, null).andExpect(status().isNotFound());
    }

    @Test
    void shouldAllowVendasOnlyToCaixaAndGerente() throws Exception {
        final String estoquistaToken = newUsuarioToken("Estoquista");
        final String gerenteToken = newUsuarioToken("Gerente");
        final Long produtoId = saveProdutoComEstoque("UN", "10.00", "10");
        final String itens = """
                {"produtoId": %d, "quantidade": 1}
                """.formatted(produtoId);

        saveVenda(estoquistaToken, "PIX", null, itens).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/venda/vendas").header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken)))
                .andExpect(status().isForbidden());

        final String json = saveVenda(gerenteToken, "CARTAO_CREDITO", null, itens)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.troco").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        cancelVenda(estoquistaToken, ((Number) JsonPath.read(json, "$.id")).longValue(), null).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/venda/vendas")).andExpect(status().isUnauthorized());
    }

    private void assertEstoque(final Long produtoId, final String quantidade) throws Exception {
        mockMvc.perform(get("/api/v1/produto/produtos/produto/{id}", produtoId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.quantidadeEstoque").value(Double.parseDouble(quantidade)));
    }

    private ResultActions saveVenda(final String accessToken, final String formaPagamento, final String valorRecebido, final String itens)
            throws Exception {
        return mockMvc.perform(post("/api/v1/venda/vendas/venda")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"formaPagamento": "%s", "valorRecebido": %s, "itens": [%s]}
                        """.formatted(formaPagamento, valorRecebido == null ? "null" : valorRecebido, itens)));
    }

    private long saveVendaId(final Long produtoId, final String quantidade) throws Exception {
        final String json = saveVenda(caixaToken, "PIX", null, """
                {"produtoId": %d, "quantidade": %s}
                """.formatted(produtoId, quantidade))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private ResultActions cancelVenda(final String accessToken, final long vendaId, final String motivo) throws Exception {
        return mockMvc.perform(put("/api/v1/venda/vendas/venda/{id}/cancelamento", vendaId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\": %s}".formatted(motivo == null ? "null" : "\"" + motivo + "\"")));
    }

    private Long saveProdutoComEstoque(final String unidadeMedida, final String precoVenda, final String estoque) throws Exception {
        final String json = mockMvc.perform(post("/api/v1/produto/produtos/produto")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Produto %s", "unidadeMedida": "%s", "precoCusto": 4.00, "precoVenda": %s, "estoqueMinimo": 0}
                                """.formatted(UUID.randomUUID(), unidadeMedida, precoVenda)))
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
