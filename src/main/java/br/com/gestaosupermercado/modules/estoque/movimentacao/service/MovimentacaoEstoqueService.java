package br.com.gestaosupermercado.modules.estoque.movimentacao.service;

import br.com.gestaosupermercado.modules.estoque.movimentacao.dto.request.AjusteEstoqueRequest;
import br.com.gestaosupermercado.modules.estoque.movimentacao.dto.request.MovimentacaoEstoqueFiltroRequest;
import br.com.gestaosupermercado.modules.estoque.movimentacao.dto.response.MovimentacaoEstoqueResponse;
import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.OrigemMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.enums.TipoMovimentacao;
import br.com.gestaosupermercado.modules.estoque.movimentacao.model.MovimentacaoEstoque;
import br.com.gestaosupermercado.shared.dto.PaginaResponse;
import java.math.BigDecimal;
import org.springframework.data.domain.Pageable;

public interface MovimentacaoEstoqueService {

    PaginaResponse<MovimentacaoEstoqueResponse> findAllMovimentacoes(MovimentacaoEstoqueFiltroRequest filtro, Pageable pageable);

    MovimentacaoEstoqueResponse findMovimentacaoById(Long id);

    MovimentacaoEstoqueResponse saveAjuste(AjusteEstoqueRequest request, Long usuarioId);

    /**
     * Ponto de entrada para Compras e Vendas: movimenta o estoque do produto e registra o histórico na
     * transação de quem chama, para que a compra/venda e o estoque sejam gravados juntos ou nenhum dos dois.
     * Falha se o produto não existir ou estiver inativo, se o estoque ficar negativo, ou se for chamado fora
     * de uma transação.
     *
     * @param referenciaId id da compra ou venda que gerou a movimentação
     */
    MovimentacaoEstoque saveMovimentacao(Long produtoId, TipoMovimentacao tipo, BigDecimal quantidade, OrigemMovimentacao origem,
                                         Long referenciaId, Long usuarioId);
}
