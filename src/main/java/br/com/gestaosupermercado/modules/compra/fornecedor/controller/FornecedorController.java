package br.com.gestaosupermercado.modules.compra.fornecedor.controller;

import br.com.gestaosupermercado.modules.compra.fornecedor.dto.request.FornecedorFiltroRequest;
import br.com.gestaosupermercado.modules.compra.fornecedor.dto.request.FornecedorRequest;
import br.com.gestaosupermercado.modules.compra.fornecedor.dto.response.FornecedorResponse;
import br.com.gestaosupermercado.modules.compra.fornecedor.service.FornecedorService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Compras - Fornecedores")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/compra/fornecedores")
public class FornecedorController {

    private final FornecedorService fornecedorService;

    @Operation(summary = "Lista fornecedores com filtros (nome, CNPJ, status) e paginação")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página retornada"),
            @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @GetMapping
    @PreAuthorize("hasAuthority('COMPRA_LISTAR')")
    public ResponseEntity<PaginaResponse<FornecedorResponse>> findAllFornecedores(
            @ParameterObject @Valid final FornecedorFiltroRequest filtro,
            @ParameterObject @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) final Pageable pageable) {
        return ResponseEntity.ok(fornecedorService.findAllFornecedores(filtro, pageable));
    }

    @Operation(summary = "Detalha um fornecedor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fornecedor encontrado"),
            @ApiResponse(responseCode = "404", description = "Fornecedor não encontrado")
    })
    @GetMapping("/fornecedor/{id}")
    @PreAuthorize("hasAuthority('COMPRA_LISTAR')")
    public ResponseEntity<FornecedorResponse> findFornecedorById(@PathVariable final Long id) {
        return ResponseEntity.ok(fornecedorService.findFornecedorById(id));
    }

    @Operation(summary = "Cadastra um fornecedor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Fornecedor criado"),
            @ApiResponse(responseCode = "409", description = "CNPJ já cadastrado"),
            @ApiResponse(responseCode = "422", description = "Dados inválidos")
    })
    @PostMapping("/fornecedor")
    @PreAuthorize("hasAuthority('COMPRA_REGISTRAR')")
    public ResponseEntity<FornecedorResponse> saveFornecedor(@RequestBody @Valid final FornecedorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fornecedorService.saveFornecedor(request));
    }

    @Operation(summary = "Edita os dados de um fornecedor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fornecedor atualizado"),
            @ApiResponse(responseCode = "404", description = "Fornecedor não encontrado"),
            @ApiResponse(responseCode = "409", description = "CNPJ já cadastrado"),
            @ApiResponse(responseCode = "422", description = "Dados inválidos")
    })
    @PutMapping("/fornecedor/{id}")
    @PreAuthorize("hasAuthority('COMPRA_REGISTRAR')")
    public ResponseEntity<FornecedorResponse> updateFornecedor(@PathVariable final Long id, @RequestBody @Valid final FornecedorRequest request) {
        return ResponseEntity.ok(fornecedorService.updateFornecedor(id, request));
    }

    @Operation(summary = "Ativa ou inativa um fornecedor; inativo não recebe novas compras")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Status alterado"),
            @ApiResponse(responseCode = "404", description = "Fornecedor não encontrado")
    })
    @PutMapping("/fornecedor/{id}/status")
    @PreAuthorize("hasAuthority('COMPRA_REGISTRAR')")
    public ResponseEntity<Void> updateStatusFornecedor(@PathVariable final Long id, @RequestBody @Valid final StatusRequest request) {
        fornecedorService.updateStatusFornecedor(id, request.ativo());
        return ResponseEntity.noContent().build();
    }
}
