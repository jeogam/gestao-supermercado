package br.com.gestaosupermercado.modules.venda.venda.service;

import br.com.gestaosupermercado.modules.venda.venda.dto.request.CancelamentoVendaRequest;
import br.com.gestaosupermercado.modules.venda.venda.dto.request.VendaFiltroRequest;
import br.com.gestaosupermercado.modules.venda.venda.dto.request.VendaRequest;
import br.com.gestaosupermercado.modules.venda.venda.dto.response.VendaResponse;
import br.com.gestaosupermercado.modules.venda.venda.dto.response.VendaResumoResponse;
import br.com.gestaosupermercado.shared.dto.PaginaResponse;
import org.springframework.data.domain.Pageable;

public interface VendaService {

    PaginaResponse<VendaResumoResponse> findAllVendas(VendaFiltroRequest filtro, Pageable pageable);

    VendaResponse findVendaById(Long id);

    VendaResponse saveVenda(VendaRequest request, Long usuarioId);

    VendaResponse cancelVenda(Long id, CancelamentoVendaRequest request, Long usuarioId);
}
