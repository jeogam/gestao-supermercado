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
public enum ErrorMessage {

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
    PERFIL_DO_SISTEMA_NAO_EDITAVEL("Perfis do sistema não podem ser alterados."),

    PRODUTO_NAO_ENCONTRADO("Produto não encontrado."),
    PRODUTO_INATIVO("O produto está inativo."),
    CODIGO_BARRAS_JA_CADASTRADO("Já existe um produto cadastrado com este código de barras."),
    QUANTIDADE_INVALIDA("Quantidade inválida: deve ser maior que zero, ter no máximo 3 casas decimais e ser inteira para produtos vendidos por unidade."),
    UNIDADE_INCOMPATIVEL_COM_ESTOQUE("Não é possível vender por unidade um produto com estoque fracionado. Ajuste o estoque antes."),
    ESTOQUE_INSUFICIENTE("Estoque insuficiente para a operação."),

    MOVIMENTACAO_NAO_ENCONTRADA("Movimentação de estoque não encontrada."),
    MOTIVO_AJUSTE_OBRIGATORIO("Informe o motivo do ajuste de estoque."),
    PERIODO_INVALIDO("A data inicial deve ser anterior ou igual à data final."),

    FORNECEDOR_NAO_ENCONTRADO("Fornecedor não encontrado."),
    FORNECEDOR_INATIVO("O fornecedor está inativo."),
    CNPJ_JA_CADASTRADO("Já existe um fornecedor cadastrado com este CNPJ."),

    COMPRA_NAO_ENCONTRADA("Compra não encontrada."),
    PRODUTO_REPETIDO_NA_COMPRA("O mesmo produto aparece mais de uma vez na compra. Junte as quantidades em um único item.");

    private final String descricao;
}
