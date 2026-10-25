package br.com.gestaosupermercado.modules.compra;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
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
 * Fluxo ponta a ponta de compras contra um PostgreSQL real: fornecedores, lançamento de compra com entrada no
 * estoque, atualização do custo, rollback quando um item falha e permissões por perfil.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.security.jwt.secret=segredo-de-teste-com-mais-de-32-caracteres",
        "app.bootstrap.admin.email=admin@mercado.com",
        "app.bootstrap.admin.senha=AdminSenha1"
})
class CompraIT {

    private static final String SENHA_PADRAO = "Senha1234";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;
    private String estoquistaToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginAccessToken("admin@mercado.com", "AdminSenha1");
        estoquistaToken = newUsuarioToken("Estoquista");
    }

    @Test
    void shouldManageFornecedoresWithNormalizedAndUniqueCnpj() throws Exception {
        final String cnpj = newCnpj();
        final String cnpjFormatado = cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." + cnpj.substring(5, 8) + "/"
                + cnpj.substring(8, 12) + "-" + cnpj.substring(12);

        final Long fornecedorId = saveFornecedor(estoquistaToken, "Atacadão Irecê", cnpjFormatado)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cnpj").value(cnpj))
                .andReturn().getResponse().getContentAsString().transform(json -> ((Number) JsonPath.read(json, "$.id")).longValue());

        saveFornecedor(estoquistaToken, "Outro", cnpj)
                .andExpect(status().isConflict());
        saveFornecedor(estoquistaToken, "CNPJ curto", "123")
                .andExpect(status().isUnprocessableContent());

        mockMvc.perform(get("/api/v1/compra/fornecedores").param("cnpj", cnpjFormatado)
                        .header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(fornecedorId));

        mockMvc.perform(put("/api/v1/compra/fornecedores/fornecedor/{id}", fornecedorId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Atacadão Irecê Ltda", "cnpj": "%s", "telefone": "(74) 99999-0000"}
                                """.formatted(cnpj)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Atacadão Irecê Ltda"))
                .andExpect(jsonPath("$.telefone").value("(74) 99999-0000"));
    }

    @Test
    void shouldRegisterCompraGivingEntradaInEstoqueAndUpdatingCusto() throws Exception {
        final Long fornecedorId = saveFornecedorId(null);
        final Long arrozId = saveProduto("UN");
        final Long queijoId = saveProduto("KG");

        final String compraJson = saveCompra(estoquistaToken, fornecedorId, "NF-001", """
                {"produtoId": %d, "quantidade": 24, "precoCustoUnitario": 19.90},
                {"produtoId": %d, "quantidade": 2.5, "precoCustoUnitario": 32.00}
                """.formatted(arrozId, queijoId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorTotal").value(557.60))
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].subtotal").value(477.60))
                .andExpect(jsonPath("$.itens[0].produtoNome").exists())
                .andReturn().getResponse().getContentAsString();
        final long compraId = ((Number) JsonPath.read(compraJson, "$.id")).longValue();

        mockMvc.perform(get("/api/v1/produto/produtos/produto/{id}", arrozId).header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken)))
                .andExpect(jsonPath("$.quantidadeEstoque").value(24))
                .andExpect(jsonPath("$.precoCusto").value(19.90));
        mockMvc.perform(get("/api/v1/produto/produtos/produto/{id}", queijoId).header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken)))
                .andExpect(jsonPath("$.quantidadeEstoque").value(2.5))
                .andExpect(jsonPath("$.precoCusto").value(32.00));

        mockMvc.perform(get("/api/v1/estoque/movimentacoes").param("produtoId", arrozId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken)))
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.conteudo[0].origem").value("COMPRA"))
                .andExpect(jsonPath("$.conteudo[0].tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.conteudo[0].referenciaId").value(compraId));

        mockMvc.perform(get("/api/v1/compra/compras/compra/{id}", compraId).header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroNotaFiscal").value("NF-001"))
                .andExpect(jsonPath("$.fornecedorId").value(fornecedorId))
                .andExpect(jsonPath("$.itens.length()").value(2));

        mockMvc.perform(get("/api/v1/compra/compras").param("fornecedorId", fornecedorId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.conteudo[0].id").value(compraId))
                .andExpect(jsonPath("$.conteudo[0].fornecedorNome").exists());
    }

    @Test
    void shouldRollbackWholeCompraWhenOneItemFails() throws Exception {
        final Long fornecedorId = saveFornecedorId(null);
        final Long validoId = saveProduto("UN");
        final Long inativoId = saveProduto("UN");
        mockMvc.perform(put("/api/v1/produto/produtos/produto/{id}/status", inativoId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\": false}"))
                .andExpect(status().isNoContent());

        saveCompra(estoquistaToken, fornecedorId, null, """
                {"produtoId": %d, "quantidade": 10, "precoCustoUnitario": 5.00},
                {"produtoId": %d, "quantidade": 10, "precoCustoUnitario": 5.00}
                """.formatted(validoId, inativoId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("O produto está inativo."));

        mockMvc.perform(get("/api/v1/produto/produtos/produto/{id}", validoId).header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken)))
                .andExpect(jsonPath("$.quantidadeEstoque").value(0))
                .andExpect(jsonPath("$.precoCusto").value(10.00));
        mockMvc.perform(get("/api/v1/compra/compras").param("fornecedorId", fornecedorId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken)))
                .andExpect(jsonPath("$.totalItens").value(0));
    }

    @Test
    void shouldRejectInvalidCompras() throws Exception {
        final Long fornecedorId = saveFornecedorId(null);
        final Long produtoId = saveProduto("UN");

        saveCompra(estoquistaToken, fornecedorId, null, """
                {"produtoId": %d, "quantidade": 1, "precoCustoUnitario": 5.00},
                {"produtoId": %d, "quantidade": 2, "precoCustoUnitario": 5.00}
                """.formatted(produtoId, produtoId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("O mesmo produto aparece mais de uma vez na compra. Junte as quantidades em um único item."));
        saveCompra(estoquistaToken, fornecedorId, null, """
                {"produtoId": %d, "quantidade": 1.5, "precoCustoUnitario": 5.00}
                """.formatted(produtoId))
                .andExpect(status().isUnprocessableContent());
        saveCompra(estoquistaToken, fornecedorId, null, "")
                .andExpect(status().isUnprocessableContent());
        saveCompra(estoquistaToken, 999_999L, null, """
                {"produtoId": %d, "quantidade": 1, "precoCustoUnitario": 5.00}
                """.formatted(produtoId))
                .andExpect(status().isNotFound());
        saveCompra(estoquistaToken, fornecedorId, null, """
                {"produtoId": %d, "quantidade": 1, "precoCustoUnitario": 5.00},
                {"produtoId": 999999, "quantidade": 1, "precoCustoUnitario": 5.00}
                """.formatted(produtoId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Produto não encontrado."));
        mockMvc.perform(get("/api/v1/produto/produtos/produto/{id}", produtoId).header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken)))
                .andExpect(jsonPath("$.quantidadeEstoque").value(0));

        mockMvc.perform(put("/api/v1/compra/fornecedores/fornecedor/{id}/status", fornecedorId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(estoquistaToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\": false}"))
                .andExpect(status().isNoContent());
        saveCompra(estoquistaToken, fornecedorId, null, """
                {"produtoId": %d, "quantidade": 1, "precoCustoUnitario": 5.00}
                """.formatted(produtoId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.mensagem").value("O fornecedor está inativo."));
    }

    @Test
    void shouldDenyComprasToCaixaAndAllowGerente() throws Exception {
        final String caixaToken = newUsuarioToken("Caixa");
        final String gerenteToken = newUsuarioToken("Gerente");
        final Long fornecedorId = saveFornecedorId(null);
        final Long produtoId = saveProduto("UN");
        final String itens = """
                {"produtoId": %d, "quantidade": 1, "precoCustoUnitario": 5.00}
                """.formatted(produtoId);

        saveCompra(caixaToken, fornecedorId, null, itens).andExpect(status().isForbidden());
        saveFornecedor(caixaToken, "Fornecedor", null).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/compra/compras").header(HttpHeaders.AUTHORIZATION, bearer(caixaToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/compra/fornecedores").header(HttpHeaders.AUTHORIZATION, bearer(caixaToken)))
                .andExpect(status().isForbidden());

        saveCompra(gerenteToken, fornecedorId, null, itens).andExpect(status().isCreated());
    }

    private ResultActions saveCompra(final String accessToken, final Long fornecedorId, final String numeroNotaFiscal, final String itens)
            throws Exception {
        final String nota = numeroNotaFiscal == null ? "null" : "\"" + numeroNotaFiscal + "\"";
        return mockMvc.perform(post("/api/v1/compra/compras/compra")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"fornecedorId": %d, "numeroNotaFiscal": %s, "itens": [%s]}
                        """.formatted(fornecedorId, nota, itens)));
    }

    private ResultActions saveFornecedor(final String accessToken, final String nome, final String cnpj) throws Exception {
        final String cnpjJson = cnpj == null ? "null" : "\"" + cnpj + "\"";
        return mockMvc.perform(post("/api/v1/compra/fornecedores/fornecedor")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome": "%s", "cnpj": %s}
                        """.formatted(nome, cnpjJson)));
    }

    private Long saveFornecedorId(final String cnpj) throws Exception {
        final String json = saveFornecedor(estoquistaToken, "Fornecedor " + UUID.randomUUID(), cnpj)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private Long saveProduto(final String unidadeMedida) throws Exception {
        final String json = mockMvc.perform(post("/api/v1/produto/produtos/produto")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Produto %s", "unidadeMedida": "%s", "precoCusto": 10.00, "precoVenda": 15.00, "estoqueMinimo": 0}
                                """.formatted(UUID.randomUUID(), unidadeMedida)))
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

    private static String newCnpj() {
        return String.format("%014d", ThreadLocalRandom.current().nextLong(1L, 99_999_999_999_999L));
    }

    private static String bearer(final String accessToken) {
        return "Bearer " + accessToken;
    }
}
