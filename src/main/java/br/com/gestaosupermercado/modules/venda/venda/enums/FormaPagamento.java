package br.com.gestaosupermercado.modules.venda.venda.enums;

/**
 * Forma de pagamento da venda. Valor novo exige migration que atualize a constraint {@code ck_venda_forma_pagamento}.
 *
 * @since 0.0.1
 */
public enum FormaPagamento {
    /** Única que aceita valor recebido e calcula troco. */
    DINHEIRO,
    CARTAO_DEBITO,
    CARTAO_CREDITO,
    PIX
}
