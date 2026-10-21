package br.com.gestaosupermercado.modules.core.permissao.enums;

/**
 * Permissões conhecidas pelo código. Cada constante é usada como authority no
 * {@code @PreAuthorize("hasAuthority('...')")} e precisa existir também na tabela {@code permissao}
 * (inserida por migration). A aplicação recusa subir se os dois lados divergirem
 * (ver {@code PermissaoCatalogoValidator}).
 *
 * @since 0.0.1
 */
public enum Permissao {

    USUARIO_LISTAR,
    USUARIO_CRIAR,
    USUARIO_EDITAR,
    USUARIO_ALTERAR_STATUS,
    USUARIO_REDEFINIR_SENHA,

    PERFIL_LISTAR,
    PERFIL_CRIAR,
    PERFIL_EDITAR,
    PERFIL_ALTERAR_STATUS,

    PERMISSAO_LISTAR
}
