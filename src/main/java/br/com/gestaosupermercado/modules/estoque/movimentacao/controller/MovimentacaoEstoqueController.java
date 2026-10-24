package br.com.gestaosupermercado.modules.estoque.movimentacao.controller;

import br.com.gestaosupermercado.modules.estoque.movimentacao.dto.request.AjusteEstoqueRequest;
import br.com.gestaosupermercado.modules.estoque.movimentacao.dto.request.MovimentacaoEstoqueFiltroRequest;
import br.com.gestaosupermercado.modules.estoque.movimentacao.dto.response.MovimentacaoEstoqueResponse;
import br.com.gestaosupermercado.modules.estoque.movimentacao.service.MovimentacaoEstoqueService;
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

@Tag(name = "Estoque - Movimentações")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/estoque/movimentacoes")
public class MovimentacaoEstoqueController {

    private final MovimentacaoEstoqueService movimentacaoEstoqueService;

    @Operation(summary = "Lista o histórico de movimentações com filtros (produto, tipo, origem, período) e paginação")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página retornada"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "422", description = "Período inválido")
    })
    @GetMapping
    @PreAuthorize("hasAuthority('ESTOQUE_LISTAR')")
    public ResponseEntity<PaginaResponse<MovimentacaoEstoqueResponse>> findAllMovimentacoes(
            @ParameterObject @Valid final MovimentacaoEstoqueFiltroRequest filtro,
            @ParameterObject @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC) final Pageable pageable) {
        return ResponseEntity.ok(movimentacaoEstoqueService.findAllMovimentacoes(filtro, pageable));
    }

    @Operation(summary = "Detalha uma movimentação")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimentação encontrada"),
            @ApiResponse(responseCode = "404", description = "Movimentação não encontrada")
    })
    @GetMapping("/movimentacao/{id}")
    @PreAuthorize("hasAuthority('ESTOQUE_LISTAR')")
    public ResponseEntity<MovimentacaoEstoqueResponse> findMovimentacaoById(@PathVariable final Long id) {
        return ResponseEntity.ok(movimentacaoEstoqueService.findMovimentacaoById(id));
    }

    @Operation(summary = "Ajusta manualmente o estoque de um produto (entrada ou saída, com motivo)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ajuste registrado"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
            @ApiResponse(responseCode = "409", description = "O produto foi movimentado ao mesmo tempo por outra operação"),
            @ApiResponse(responseCode = "422", description = "Dados inválidos, produto inativo ou estoque insuficiente")
    })
    @PostMapping("/ajuste")
    @PreAuthorize("hasAuthority('ESTOQUE_AJUSTAR')")
    public ResponseEntity<MovimentacaoEstoqueResponse> saveAjuste(@RequestBody @Valid final AjusteEstoqueRequest request,
                                                                  @AuthenticationPrincipal final Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(movimentacaoEstoqueService.saveAjuste(request, Long.valueOf(jwt.getSubject())));
    }
}
