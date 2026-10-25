package br.com.gestaosupermercado.modules.compra.compra.controller;

import br.com.gestaosupermercado.modules.compra.compra.dto.request.CompraFiltroRequest;
import br.com.gestaosupermercado.modules.compra.compra.dto.request.CompraRequest;
import br.com.gestaosupermercado.modules.compra.compra.dto.response.CompraResponse;
import br.com.gestaosupermercado.modules.compra.compra.dto.response.CompraResumoResponse;
import br.com.gestaosupermercado.modules.compra.compra.service.CompraService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Compras")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/compra/compras")
public class CompraController {

    private final CompraService compraService;

    @Operation(summary = "Lista compras com filtros (fornecedor, nota fiscal, período) e paginação")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página retornada"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "422", description = "Período inválido")
    })
    @GetMapping
    @PreAuthorize("hasAuthority('COMPRA_LISTAR')")
    public ResponseEntity<PaginaResponse<CompraResumoResponse>> findAllCompras(
            @ParameterObject @Valid final CompraFiltroRequest filtro,
            @ParameterObject @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC) final Pageable pageable) {
        return ResponseEntity.ok(compraService.findAllCompras(filtro, pageable));
    }

    @Operation(summary = "Detalha uma compra com seus itens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compra encontrada"),
            @ApiResponse(responseCode = "404", description = "Compra não encontrada")
    })
    @GetMapping("/compra/{id}")
    @PreAuthorize("hasAuthority('COMPRA_LISTAR')")
    public ResponseEntity<CompraResponse> findCompraById(@PathVariable final Long id) {
        return ResponseEntity.ok(compraService.findCompraById(id));
    }

    @Operation(summary = "Lança uma compra: dá entrada no estoque de cada item e atualiza o preço de custo dos produtos")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compra registrada"),
            @ApiResponse(responseCode = "404", description = "Fornecedor ou produto não encontrado"),
            @ApiResponse(responseCode = "409", description = "Um produto foi movimentado ao mesmo tempo por outra operação"),
            @ApiResponse(responseCode = "422", description = "Dados inválidos, fornecedor/produto inativo ou produto repetido")
    })
    @PostMapping("/compra")
    @PreAuthorize("hasAuthority('COMPRA_REGISTRAR')")
    public ResponseEntity<CompraResponse> saveCompra(@RequestBody @Valid final CompraRequest request,
                                                     @AuthenticationPrincipal final Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compraService.saveCompra(request, Long.valueOf(jwt.getSubject())));
    }
}
