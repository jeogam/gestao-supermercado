package br.com.gestaosupermercado.modules.core.permissao.controller;

import br.com.gestaosupermercado.modules.core.permissao.dto.response.PermissaoResponse;
import br.com.gestaosupermercado.modules.core.permissao.service.PermissaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Permissões")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/core/permissoes")
public class PermissaoController {

    private final PermissaoService permissaoService;

    @Operation(summary = "Lista o catálogo de permissões disponíveis para montar perfis")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catálogo retornado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSAO_LISTAR')")
    public ResponseEntity<List<PermissaoResponse>> findAllPermissoes() {
        return ResponseEntity.ok(permissaoService.findAllPermissoes());
    }
}
