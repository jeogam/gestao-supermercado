package br.com.gestaosupermercado.modules.venda.venda.controller;

import br.com.gestaosupermercado.modules.venda.venda.dto.request.CancelamentoVendaRequest;
import br.com.gestaosupermercado.modules.venda.venda.dto.request.VendaFiltroRequest;
import br.com.gestaosupermercado.modules.venda.venda.dto.request.VendaRequest;
import br.com.gestaosupermercado.modules.venda.venda.dto.response.VendaResponse;
import br.com.gestaosupermercado.modules.venda.venda.dto.response.VendaResumoResponse;
import br.com.gestaosupermercado.modules.venda.venda.service.VendaService;
import br.com.gestaosupermercado.shared.dto.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Vendas")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/venda/vendas")
public class VendaController {

    private final VendaService vendaService;

    @Operation(summary = "Lista vendas com filtros (status, forma de pagamento, operador, período) e paginação")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página retornada"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "422", description = "Período inválido")
    })
    @GetMapping
    @PreAuthorize("hasAuthority('VENDA_LISTAR')")
    public ResponseEntity<PaginaResponse<VendaResumoResponse>> findAllVendas(
            @ParameterObject @Valid final VendaFiltroRequest filtro,
            @ParameterObject @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC) final Pageable pageable) {
        return ResponseEntity.ok(vendaService.findAllVendas(filtro, pageable));
    }

    @Operation(summary = "Detalha uma venda com seus itens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venda encontrada"),
            @ApiResponse(responseCode = "404", description = "Venda não encontrada")
    })
    @GetMapping("/venda/{id}")
    @PreAuthorize("hasAuthority('VENDA_LISTAR')")
    public ResponseEntity<VendaResponse> findVendaById(@PathVariable final Long id) {
        return ResponseEntity.ok(vendaService.findVendaById(id));
    }

    @Operation(summary = "Registra uma venda: usa o preço do cadastro e dá baixa no estoque de cada item")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Venda registrada"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
            @ApiResponse(responseCode = "409", description = "Um produto foi movimentado ao mesmo tempo por outra operação"),
            @ApiResponse(responseCode = "422", description = "Dados inválidos, produto inativo, estoque insuficiente ou valor recebido menor que o total")
    })
    @PostMapping("/venda")
    @PreAuthorize("hasAuthority('VENDA_REGISTRAR')")
    public ResponseEntity<VendaResponse> saveVenda(@RequestBody @Valid final VendaRequest request, @AuthenticationPrincipal final Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vendaService.saveVenda(request, Long.valueOf(jwt.getSubject())));
    }

    @Operation(summary = "Cancela (ou desfaz) uma venda e devolve os itens ao estoque")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venda cancelada"),
            @ApiResponse(responseCode = "404", description = "Venda não encontrada"),
            @ApiResponse(responseCode = "409", description = "A venda ou um produto foi alterado ao mesmo tempo por outra operação"),
            @ApiResponse(responseCode = "422", description = "A venda já está cancelada")
    })
    @PutMapping("/venda/{id}/cancelamento")
    @PreAuthorize("hasAuthority('VENDA_CANCELAR')")
    public ResponseEntity<VendaResponse> cancelVenda(@PathVariable final Long id, @RequestBody @Valid final CancelamentoVendaRequest request,
                                                     @AuthenticationPrincipal final Jwt jwt) {
        return ResponseEntity.ok(vendaService.cancelVenda(id, request, Long.valueOf(jwt.getSubject())));
    }
}
