package br.com.gestaosupermercado.modules.produto.produto.controller;

import br.com.gestaosupermercado.modules.core.permissao.enums.Permissao;
import br.com.gestaosupermercado.modules.produto.produto.dto.request.ProdutoFiltroRequest;
import br.com.gestaosupermercado.modules.produto.produto.dto.request.ProdutoRequest;
import br.com.gestaosupermercado.modules.produto.produto.dto.response.ProdutoResponse;
import br.com.gestaosupermercado.modules.produto.produto.service.ProdutoService;
import br.com.gestaosupermercado.shared.dto.PaginaResponse;
import br.com.gestaosupermercado.shared.dto.StatusRequest;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Produtos")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/produto/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    @Operation(summary = "Lista produtos com filtros (nome, código de barras, status, reposição) e paginação")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página retornada"),
            @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @GetMapping
    @PreAuthorize("hasAuthority('PRODUTO_LISTAR')")
    public ResponseEntity<PaginaResponse<ProdutoResponse>> findAllProdutos(
            @ParameterObject @Valid final ProdutoFiltroRequest filtro,
            @ParameterObject @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) final Pageable pageable,
            final Authentication authentication) {
        return ResponseEntity.ok(produtoService.findAllProdutos(filtro, pageable, canGerenciarProduto(authentication)));
    }

    @Operation(summary = "Detalha um produto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto encontrado"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    @GetMapping("/produto/{id}")
    @PreAuthorize("hasAuthority('PRODUTO_LISTAR')")
    public ResponseEntity<ProdutoResponse> findProdutoById(@PathVariable final Long id, final Authentication authentication) {
        return ResponseEntity.ok(produtoService.findProdutoById(id, canGerenciarProduto(authentication)));
    }

    @Operation(summary = "Cadastra um produto (estoque inicial zero)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produto criado"),
            @ApiResponse(responseCode = "409", description = "Código de barras já cadastrado"),
            @ApiResponse(responseCode = "422", description = "Dados inválidos")
    })
    @PostMapping("/produto")
    @PreAuthorize("hasAuthority('PRODUTO_GERENCIAR')")
    public ResponseEntity<ProdutoResponse> saveProduto(@RequestBody @Valid final ProdutoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.saveProduto(request));
    }

    @Operation(summary = "Edita os dados de um produto (não altera a quantidade em estoque)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto atualizado"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
            @ApiResponse(responseCode = "409", description = "Código de barras já cadastrado"),
            @ApiResponse(responseCode = "422", description = "Dados inválidos")
    })
    @PutMapping("/produto/{id}")
    @PreAuthorize("hasAuthority('PRODUTO_GERENCIAR')")
    public ResponseEntity<ProdutoResponse> updateProduto(@PathVariable final Long id, @RequestBody @Valid final ProdutoRequest request) {
        return ResponseEntity.ok(produtoService.updateProduto(id, request));
    }

    @Operation(summary = "Ativa ou inativa um produto; inativo não pode ser vendido nem movimentado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Status alterado"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    @PutMapping("/produto/{id}/status")
    @PreAuthorize("hasAuthority('PRODUTO_GERENCIAR')")
    public ResponseEntity<Void> updateStatusProduto(@PathVariable final Long id, @RequestBody @Valid final StatusRequest request) {
        produtoService.updateStatusProduto(id, request.ativo());
        return ResponseEntity.noContent().build();
    }

    private static boolean canGerenciarProduto(final Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> Permissao.PRODUTO_GERENCIAR.name().equals(authority.getAuthority()));
    }
}
