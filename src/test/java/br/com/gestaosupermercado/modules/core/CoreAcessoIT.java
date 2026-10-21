package br.com.gestaosupermercado.modules.core;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Fluxo ponta a ponta de acesso contra um PostgreSQL real (migrations Flyway incluídas): login, RBAC
 * dinâmico, inativação, rotação de refresh token e proteção do último Administrador.
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.security.jwt.secret=segredo-de-teste-com-mais-de-32-caracteres",
        "app.bootstrap.admin.email=admin@mercado.com",
        "app.bootstrap.admin.senha=AdminSenha1"
})
class CoreAcessoIT {

    private static final String ADMIN_EMAIL = "admin@mercado.com";
    private static final String ADMIN_SENHA = "AdminSenha1";
    private static final String SENHA_PADRAO = "Senha1234";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn401WithJsonBodyWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/core/usuarios/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldReturnAllPermissoesForBootstrapAdministrador() throws Exception {
        final String accessToken = loginAccessToken(ADMIN_EMAIL, ADMIN_SENHA);

        mockMvc.perform(get("/api/v1/core/usuarios/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.permissoes", Matchers.hasItems("USUARIO_CRIAR", "PERFIL_EDITAR", "PERMISSAO_LISTAR")));
    }

    @Test
    void shouldApplyPerfilChangeImmediatelyWithoutNewLogin() throws Exception {
        final String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_SENHA);
        final String email = newEmail();
        final Long usuarioId = saveUsuario(adminToken, email, findPerfilIdByNome(adminToken, "Caixa"));
        final String caixaToken = loginAccessToken(email, SENHA_PADRAO);

        mockMvc.perform(get("/api/v1/core/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(caixaToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/core/usuarios/usuario/{id}", usuarioId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Operador", "email": "%s", "perfilIds": [%d]}
                                """.formatted(email, findPerfilIdByNome(adminToken, "Gerente"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/core/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(caixaToken)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldInvalidateTokensAndLoginWhenUsuarioIsInativado() throws Exception {
        final String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_SENHA);
        final String email = newEmail();
        final Long usuarioId = saveUsuario(adminToken, email, findPerfilIdByNome(adminToken, "Estoquista"));
        final String usuarioToken = loginAccessToken(email, SENHA_PADRAO);

        mockMvc.perform(put("/api/v1/core/usuarios/usuario/{id}/status", usuarioId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\": false}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/core/usuarios/me").header(HttpHeaders.AUTHORIZATION, bearer(usuarioToken)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, SENHA_PADRAO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRotateRefreshTokenAndRevokeAllOnReuse() throws Exception {
        final String loginJson = login(ADMIN_EMAIL, ADMIN_SENHA);
        final String refreshOriginal = JsonPath.read(loginJson, "$.refreshToken");

        final String refreshJson = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(refreshOriginal)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        final String refreshNovo = JsonPath.read(refreshJson, "$.refreshToken");

        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(refreshOriginal)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(refreshBody(refreshNovo)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotAllowRemovingTheLastAdministrador() throws Exception {
        final String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_SENHA);
        final String meJson = mockMvc.perform(get("/api/v1/core/usuarios/me").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andReturn().getResponse().getContentAsString();
        final Long adminId = ((Number) JsonPath.read(meJson, "$.id")).longValue();

        mockMvc.perform(put("/api/v1/core/usuarios/usuario/{id}", adminId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Administrador", "email": "%s", "perfilIds": [%d]}
                                """.formatted(ADMIN_EMAIL, findPerfilIdByNome(adminToken, "Caixa"))))
                .andExpect(status().isUnprocessableContent());

        mockMvc.perform(put("/api/v1/core/usuarios/usuario/{id}/status", adminId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\": false}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void shouldRejectEditingPerfilDoSistema() throws Exception {
        final String adminToken = loginAccessToken(ADMIN_EMAIL, ADMIN_SENHA);

        mockMvc.perform(put("/api/v1/core/perfis/perfil/{id}/status", findPerfilIdByNome(adminToken, "Administrador"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\": false}"))
                .andExpect(status().isUnprocessableContent());
    }

    private String login(final String email, final String senha) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, senha)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String loginAccessToken(final String email, final String senha) throws Exception {
        return JsonPath.read(login(email, senha), "$.accessToken");
    }

    private Long findPerfilIdByNome(final String accessToken, final String nome) throws Exception {
        final String json = mockMvc.perform(get("/api/v1/core/perfis").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        final List<Number> ids = JsonPath.read(json, "$[?(@.nome == '" + nome + "')].id");
        return ids.getFirst().longValue();
    }

    private Long saveUsuario(final String accessToken, final String email, final Long perfilId) throws Exception {
        final String json = mockMvc.perform(post("/api/v1/core/usuarios/usuario")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Operador", "email": "%s", "senha": "%s", "perfilIds": [%d]}
                                """.formatted(email, SENHA_PADRAO, perfilId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private static String newEmail() {
        return "operador-" + UUID.randomUUID() + "@mercado.com";
    }

    private static String bearer(final String accessToken) {
        return "Bearer " + accessToken;
    }

    private static String loginBody(final String email, final String senha) {
        return "{\"email\": \"%s\", \"senha\": \"%s\"}".formatted(email, senha);
    }

    private static String refreshBody(final String refreshToken) {
        return "{\"refreshToken\": \"%s\"}".formatted(refreshToken);
    }
}
