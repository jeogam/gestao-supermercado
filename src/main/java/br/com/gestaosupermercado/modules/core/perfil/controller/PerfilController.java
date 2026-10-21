package br.com.gestaosupermercado.modules.core.perfil.controller;

import br.com.gestaosupermercado.modules.core.perfil.dto.request.PerfilRequest;
import br.com.gestaosupermercado.modules.core.perfil.dto.response.PerfilResponse;
import br.com.gestaosupermercado.modules.core.perfil.service.PerfilService;
import br.com.gestaosupermercado.shared.dto.StatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Perfis")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/core/perfis")
public class PerfilController {

    private final PerfilService perfilService;

    @Operation(summary = "Lista os perfis, opcionalmente filtrando por status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfis retornados"),
            @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @GetMapping
    @PreAuthorize("hasAuthority('PERFIL_LISTAR')")
    public ResponseEntity<List<PerfilResponse>> findAllPerfis(
            @Parameter(description = "Filtra por ativo/inativo; omitido retorna todos")
            @RequestParam(required = false) final Boolean ativo) {
        return ResponseEntity.ok(perfilService.findAllPerfis(ativo));
    }

    @Operation(summary = "Detalha um perfil com suas permissões")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil encontrado"),
            @ApiResponse(responseCode = "404", description = "Perfil não encontrado")
    })
    @GetMapping("/perfil/{id}")
    @PreAuthorize("hasAuthority('PERFIL_LISTAR')")
    public ResponseEntity<PerfilResponse> findPerfilById(@PathVariable final Long id) {
        return ResponseEntity.ok(perfilService.findPerfilById(id));
    }

    @Operation(summary = "Cadastra um perfil")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Perfil criado"),
            @ApiResponse(responseCode = "409", description = "Nome já cadastrado"),
            @ApiResponse(responseCode = "422", description = "Dados inválidos")
    })
    @PostMapping("/perfil")
    @PreAuthorize("hasAuthority('PERFIL_CRIAR')")
    public ResponseEntity<PerfilResponse> savePerfil(@RequestBody @Valid final PerfilRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(perfilService.savePerfil(request));
    }

    @Operation(summary = "Edita nome, descrição e permissões de um perfil")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado"),
            @ApiResponse(responseCode = "404", description = "Perfil não encontrado"),
            @ApiResponse(responseCode = "409", description = "Nome já cadastrado"),
            @ApiResponse(responseCode = "422", description = "Dados inválidos ou perfil do sistema")
    })
    @PutMapping("/perfil/{id}")
    @PreAuthorize("hasAuthority('PERFIL_EDITAR')")
    public ResponseEntity<PerfilResponse> updatePerfil(@PathVariable final Long id, @RequestBody @Valid final PerfilRequest request) {
        return ResponseEntity.ok(perfilService.updatePerfil(id, request));
    }

    @Operation(summary = "Ativa ou inativa um perfil")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Status alterado"),
            @ApiResponse(responseCode = "404", description = "Perfil não encontrado"),
            @ApiResponse(responseCode = "422", description = "Perfil do sistema")
    })
    @PutMapping("/perfil/{id}/status")
    @PreAuthorize("hasAuthority('PERFIL_ALTERAR_STATUS')")
    public ResponseEntity<Void> updateStatusPerfil(@PathVariable final Long id, @RequestBody @Valid final StatusRequest request) {
        perfilService.updateStatusPerfil(id, request.ativo());
        return ResponseEntity.noContent().build();
    }
}
