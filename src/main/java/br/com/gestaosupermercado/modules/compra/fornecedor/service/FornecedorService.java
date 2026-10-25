package br.com.gestaosupermercado.modules.compra.fornecedor.service;

import br.com.gestaosupermercado.modules.compra.fornecedor.dto.request.FornecedorFiltroRequest;
import br.com.gestaosupermercado.modules.compra.fornecedor.dto.request.FornecedorRequest;
import br.com.gestaosupermercado.modules.compra.fornecedor.dto.response.FornecedorResponse;
import br.com.gestaosupermercado.modules.compra.fornecedor.model.Fornecedor;
import br.com.gestaosupermercado.shared.dto.PaginaResponse;
import org.springframework.data.domain.Pageable;

public interface FornecedorService {

    PaginaResponse<FornecedorResponse> findAllFornecedores(FornecedorFiltroRequest filtro, Pageable pageable);

    FornecedorResponse findFornecedorById(Long id);

    FornecedorResponse saveFornecedor(FornecedorRequest request);

    FornecedorResponse updateFornecedor(Long id, FornecedorRequest request);

    void updateStatusFornecedor(Long id, boolean ativo);

    /**
     * Fornecedor para vincular a uma nova compra. Falha se não existir ou estiver inativo.
     */
    Fornecedor findFornecedorAtivoForCompra(Long id);
}
