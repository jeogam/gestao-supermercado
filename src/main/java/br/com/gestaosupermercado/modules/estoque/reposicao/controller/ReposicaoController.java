package br.com.gestaosupermercado.modules.estoque.reposicao.controller;

import br.com.gestaosupermercado.modules.estoque.reposicao.dto.response.ReposicaoResponse;
import br.com.gestaosupermercado.modules.estoque.reposicao.service.ReposicaoService;
import br.com.gestaosupermercado.shared.dto.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Estoque - Reposição")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/estoque/reposicao")
public class ReposicaoController {

    private final ReposicaoService reposicaoService;

    @Operation(summary = "Lista os produtos ativos abaixo do estoque mínimo, com a quantidade que falta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página retornada"),
            @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @GetMapping
    @PreAuthorize("hasAuthority('ESTOQUE_LISTAR')")
    public ResponseEntity<PaginaResponse<ReposicaoResponse>> findAllProdutosParaRepor(
            @ParameterObject @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) final Pageable pageable) {
        return ResponseEntity.ok(reposicaoService.findAllProdutosParaRepor(pageable));
    }
}
