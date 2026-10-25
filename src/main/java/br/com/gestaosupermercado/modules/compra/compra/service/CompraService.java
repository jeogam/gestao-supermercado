package br.com.gestaosupermercado.modules.compra.compra.service;

import br.com.gestaosupermercado.modules.compra.compra.dto.request.CompraFiltroRequest;
import br.com.gestaosupermercado.modules.compra.compra.dto.request.CompraRequest;
import br.com.gestaosupermercado.modules.compra.compra.dto.response.CompraResponse;
import br.com.gestaosupermercado.modules.compra.compra.dto.response.CompraResumoResponse;
import br.com.gestaosupermercado.shared.dto.PaginaResponse;
import org.springframework.data.domain.Pageable;

public interface CompraService {

    PaginaResponse<CompraResumoResponse> findAllCompras(CompraFiltroRequest filtro, Pageable pageable);

    CompraResponse findCompraById(Long id);

    CompraResponse saveCompra(CompraRequest request, Long usuarioId);
}
