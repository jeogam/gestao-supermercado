package br.com.gestaosupermercado.shared.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Catálogo central das mensagens de erro de negócio. Centralizar evita textos duplicados ou divergentes
 * para a mesma situação em services diferentes.
 *
 * @since 0.0.1
 */
@Getter
@RequiredArgsConstructor
public enum MensagemErro {

    CREDENCIAIS_INVALIDAS("E-mail ou senha inválidos."),
    REFRESH_TOKEN_INVALIDO("Sessão expirada ou inválida. Faça login novamente."),
    SENHA_ATUAL_INCORRETA("A senha atual informada está incorreta."),
    NOVA_SENHA_IGUAL_ATUAL("A nova senha deve ser diferente da senha atual."),

    USUARIO_NAO_ENCONTRADO("Usuário não encontrado."),
    EMAIL_JA_CADASTRADO("Já existe um usuário cadastrado com este e-mail."),
    USUARIO_NAO_PODE_ALTERAR_PROPRIO_STATUS("Você não pode ativar ou inativar o seu próprio usuário."),
    ULTIMO_SUPERADMIN("A operação deixaria o sistema sem nenhum Superadmin ativo."),

    PERFIL_NAO_ENCONTRADO("Perfil não encontrado."),
    PERFIL_INATIVO("Não é possível vincular um perfil inativo."),
    PERFIL_JA_CADASTRADO("Já existe um perfil cadastrado com este nome."),
    PERFIL_DO_SISTEMA_NAO_EDITAVEL("Perfis do sistema não podem ser alterados.");

    private final String descricao;
}
