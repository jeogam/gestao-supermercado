package br.com.gestaosupermercado.modules.estoque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.OrigemMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.TipoMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.service.MovimentacaoEstoqueService;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.IllegalTransactionStateException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Fluxo ponta a ponta do estoque contra um PostgreSQL real: ajustes, histórico, alerta de reposição e
 * permissões por perfil.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.security.jwt.secret=segredo-de-teste-com-mais-de-32-caracteres",
        "app.bootstrap.admin.email=admin@mercado.com",
        "app.bootstrap.admin.senha=AdminSenha1"
})
class EstoqueIT {

    private static final String SENHA_PADRAO = "Senha1234";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovimentacaoEstoqueService movimentacaoEstoqueService;

    private String adminToken;
    private String estoquistaToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginAccessToken("admin@mercado.com", "AdminSenha1");
        estoquistaToken = newUsuarioToken("Estoquista");
    }

    @Test
    void shouldAdjustEstoqueAndKeepHistorico() throws Exception {
        final Long produtoId = saveProduto("UN", "10");

        saveAjuste(estoquistaToken, produtoId, "ENTRADA", "12", "Contagem de inventário")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldoApos").value(12))
                .andExpect(jsonPath("$.origem").value("AJUSTE"));
        saveAjuste(estoquistaToken, produtoId, "SAIDA", "3", "Embalagens avariadas")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldoApos").value(9));

        mockMvc.perform(get("/api/v1/produto/produtos/produto/{id}", produtoId).header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeEstoque").value(9));

        final String historicoJson = mockMvc.perform(get("/api/v1/estoque/movimentacoes")
                        .param("produtoId", produtoId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(2))
                // Mais recente primeiro.
                .andExpect(jsonPath("$.conteudo[0].tipo").value("SAIDA"))
                .andExpect(jsonPath("$.conteudo[0].motivo").value("Embalagens avariadas"))
                .andExpect(jsonPath("$.conteudo[0].produtoNome", Matchers.startsWith("Produto ")))
                .andExpect(jsonPath("$.conteudo[1].tipo").value("ENTRADA"))
                .andReturn().getResponse().getContentAsString();

        final Number movimentacaoId = JsonPath.read(historicoJson, "$.conteudo[0].id");
        mockMvc.perform(get("/api/v1/estoque/movimentacoes/movimentacao/{id}", movimentacaoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoApos").value(9));

        mockMvc.perform(get("/api/v1/estoque/movimentacoes")
                        .param("produtoId", produtoId.toString())
                        .param("tipo", "ENTRADA")
                        .param("dataInicio", "2000-01-01T00:00:00Z")
                        .header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(1));
    }

    @Test
    void shouldRejectInvalidAjusteWithoutChangingEstoque() throws Exception {
        final Long produtoId = saveProduto("UN", "0");
        saveAjuste(estoquistaToken, produtoId, "ENTRADA", "2", "Inventário").andExpect(status().isCreated());

        saveAjuste(estoquistaToken, produtoId, "SAIDA", "5", "Avaria")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("Estoque insuficiente para a operação."));
        saveAjuste(estoquistaToken, produtoId, "ENTRADA", "1.5", "Inventário").andExpect(status().isUnprocessableContent());
        saveAjuste(estoquistaToken, produtoId, "ENTRADA", "1", " ").andExpect(status().isUnprocessableContent());
        saveAjuste(estoquistaToken, produtoId, "ENTRADA", "-1", "Inventário").andExpect(status().isUnprocessableContent());
        saveAjuste(estoquistaToken, 999_999L, "ENTRADA", "1", "Inventário").andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/estoque/movimentacoes")
                        .param("produtoId", produtoId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken)))
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.conteudo[0].saldoApos").value(2));
    }

    @Test
    void shouldRejectAjusteOfProdutoInativo() throws Exception {
        final Long produtoId = saveProduto("UN", "0");
        mockMvc.perform(put("/api/v1/produto/produtos/produto/{id}/status", produtoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\": false}"))
                .andExpect(status().isNoContent());

        saveAjuste(estoquistaToken, produtoId, "ENTRADA", "1", "Inventário")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("O produto está inativo."));
    }

    @Test
    void shouldListOnlyAtivosAbaixoDoMinimoInReposicao() throws Exception {
        final Long abaixoId = saveProduto("KG", "10");
        saveAjuste(estoquistaToken, abaixoId, "ENTRADA", "2.5", "Recebimento").andExpect(status().isCreated());
        final Long suficienteId = saveProduto("UN", "1");
        saveAjuste(estoquistaToken, suficienteId, "ENTRADA", "5", "Recebimento").andExpect(status().isCreated());
        final Long inativoId = saveProduto("UN", "10");
        mockMvc.perform(put("/api/v1/produto/produtos/produto/{id}/status", inativoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\": false}"))
                .andExpect(status().isNoContent());

        final String json = mockMvc.perform(get("/api/v1/estoque/reposicao")
                        .param("size", "200")
                        .header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        final List<Number> ids = JsonPath.read(json, "$.conteudo[*].produtoId");
        assertThat(ids.stream().map(Number::longValue).toList())
                .contains(abaixoId)
                .doesNotContain(suficienteId, inativoId);
        final List<Number> faltante = JsonPath.read(json, "$.conteudo[?(@.produtoId == " + abaixoId + ")].quantidadeFaltante");
        assertThat(new BigDecimal(faltante.getFirst().toString())).isEqualByComparingTo("7.5");
    }

    @Test
    void shouldDenyEstoqueToCaixa() throws Exception {
        final String caixaToken = newUsuarioToken("Caixa");
        final Long produtoId = saveProduto("UN", "0");

        saveAjuste(caixaToken, produtoId, "ENTRADA", "1", "Inventário").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/estoque/movimentacoes").header(HttpHeaders.AUTHORIZATION, bearer(caixaToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/estoque/reposicao").header(HttpHeaders.AUTHORIZATION, bearer(caixaToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/estoque/reposicao"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowGerenteToAdjustEstoque() throws Exception {
        final String gerenteToken = newUsuarioToken("Gerente");
        final Long produtoId = saveProduto("UN", "0");

        saveAjuste(gerenteToken, produtoId, "ENTRADA", "4", "Inventário").andExpect(status().isCreated());
    }

    @Test
    void shouldRejectPeriodoInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/estoque/movimentacoes")
                        .param("dataInicio", "2026-10-31T00:00:00Z")
                        .param("dataFim", "2026-10-01T00:00:00Z")
                        .header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void shouldRequireTransactionForMovimentacaoFromOtherModules() {
        assertThatThrownBy(() -> movimentacaoEstoqueService.saveMovimentacao(1L, TipoMovimentacao.SAIDA, BigDecimal.ONE,
                OrigemMovimentacao.VENDA, 1L, 1L))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private ResultActions saveAjuste(final String accessToken, final Long produtoId, final String tipo, final String quantidade,
                                     final String motivo) throws Exception {
        return mockMvc.perform(post("/api/v1/estoque/movimentacoes/ajuste")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"produtoId": %d, "tipo": "%s", "quantidade": %s, "motivo": "%s"}
                        """.formatted(produtoId, tipo, quantidade, motivo)));
    }

    private Long saveProduto(final String unidadeMedida, final String estoqueMinimo) throws Exception {
        final String json = mockMvc.perform(post("/api/v1/produto/produtos/produto")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Produto %s", "unidadeMedida": "%s", "precoCusto": 10.00, "precoVenda": 15.00, "estoqueMinimo": %s}
                                """.formatted(UUID.randomUUID(), unidadeMedida, estoqueMinimo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
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
