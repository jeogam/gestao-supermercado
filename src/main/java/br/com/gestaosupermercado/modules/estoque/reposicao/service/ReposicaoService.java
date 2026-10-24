package br.com.gestaosupermercado.modules.estoque.reposicao.service;

import br.com.gestaosupermercado.modules.estoque.reposicao.dto.response.ReposicaoResponse;
import br.com.gestaosupermercado.shared.dto.PaginaResponse;
import org.springframework.data.domain.Pageable;

public interface ReposicaoService {

    PaginaResponse<ReposicaoResponse> findAllProdutosParaRepor(Pageable pageable);
}
