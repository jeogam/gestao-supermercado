package br.com.gestaosupermercado.modules.core.usuario.controller;

import br.com.gestaosupermercado.modules.core.usuario.dto.request.SenhaAlteracaoRequest;
import br.com.gestaosupermercado.modules.core.usuario.dto.request.SenhaRedefinicaoRequest;
import br.com.gestaosupermercado.modules.core.usuario.dto.request.UsuarioCreateRequest;
import br.com.gestaosupermercado.modules.core.usuario.dto.request.UsuarioFiltroRequest;
import br.com.gestaosupermercado.modules.core.usuario.dto.request.UsuarioUpdateRequest;
import br.com.gestaosupermercado.modules.core.usuario.dto.response.UsuarioLogadoResponse;
import br.com.gestaosupermercado.modules.core.usuario.dto.response.UsuarioResponse;
import br.com.gestaosupermercado.modules.core.usuario.service.UsuarioService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Usuários")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/core/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(summary = "Lista usuários com filtros e paginação")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página retornada"),
            @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @GetMapping
    @PreAuthorize("hasAuthority('USUARIO_LISTAR')")
    public ResponseEntity<PaginaResponse<UsuarioResponse>> findAllUsuarios(
            @ParameterObject @Valid final UsuarioFiltroRequest filtro,
            @ParameterObject @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) final Pageable pageable) {
        return ResponseEntity.ok(usuarioService.findAllUsuarios(filtro, pageable));
    }

    @Operation(summary = "Detalha um usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    @GetMapping("/usuario/{id}")
    @PreAuthorize("hasAuthority('USUARIO_LISTAR')")
    public ResponseEntity<UsuarioResponse> findUsuarioById(@PathVariable final Long id) {
        return ResponseEntity.ok(usuarioService.findUsuarioById(id));
    }

    @Operation(summary = "Cadastra um usuário com senha inicial e perfis")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado"),
            @ApiResponse(responseCode = "404", description = "Perfil não encontrado"),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado"),
            @ApiResponse(responseCode = "422", description = "Dados inválidos ou perfil inativo")
    })
    @PostMapping("/usuario")
    @PreAuthorize("hasAuthority('USUARIO_CRIAR')")
    public ResponseEntity<UsuarioResponse> saveUsuario(@RequestBody @Valid final UsuarioCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.saveUsuario(request));
    }

    @Operation(summary = "Edita nome, e-mail e perfis de um usuário")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado"),
            @ApiResponse(responseCode = "404", description = "Usuário ou perfil não encontrado"),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado"),
            @ApiResponse(responseCode = "422", description = "Dados inválidos, perfil inativo ou remoção do último Superadmin")
    })
    @PutMapping("/usuario/{id}")
    @PreAuthorize("hasAuthority('USUARIO_EDITAR')")
    public ResponseEntity<UsuarioResponse> updateUsuario(@PathVariable final Long id, @RequestBody @Valid final UsuarioUpdateRequest request) {
        return ResponseEntity.ok(usuarioService.updateUsuario(id, request));
    }

    @Operation(summary = "Ativa ou inativa um usuário; inativar encerra as sessões dele")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Status alterado"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "422", description = "Próprio usuário ou último Superadmin")
    })
    @PutMapping("/usuario/{id}/status")
    @PreAuthorize("hasAuthority('USUARIO_ALTERAR_STATUS')")
    public ResponseEntity<Void> updateStatusUsuario(@PathVariable final Long id, @RequestBody @Valid final StatusRequest request,
                                                    @AuthenticationPrincipal final Jwt jwt) {
        usuarioService.updateStatusUsuario(id, request.ativo(), Long.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Redefine a senha de um usuário e encerra as sessões dele")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha redefinida"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado"),
            @ApiResponse(responseCode = "422", description = "Senha fora da política")
    })
    @PutMapping("/usuario/{id}/senha")
    @PreAuthorize("hasAuthority('USUARIO_REDEFINIR_SENHA')")
    public ResponseEntity<Void> updateSenhaUsuario(@PathVariable final Long id, @RequestBody @Valid final SenhaRedefinicaoRequest request) {
        usuarioService.updateSenhaUsuario(id, request.novaSenha());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Dados, perfis e permissões efetivas do usuário autenticado")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Usuário autenticado"))
    @GetMapping("/me")
    public ResponseEntity<UsuarioLogadoResponse> findUsuarioLogado(@AuthenticationPrincipal final Jwt jwt) {
        return ResponseEntity.ok(usuarioService.findUsuarioLogado(Long.valueOf(jwt.getSubject())));
    }

    @Operation(summary = "Troca a própria senha; todas as sessões são encerradas")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha alterada"),
            @ApiResponse(responseCode = "422", description = "Senha atual incorreta ou nova senha fora da política")
    })
    @PutMapping("/me/senha")
    public ResponseEntity<Void> updateSenhaUsuarioLogado(@RequestBody @Valid final SenhaAlteracaoRequest request,
                                                         @AuthenticationPrincipal final Jwt jwt) {
        usuarioService.updateSenhaUsuarioLogado(Long.valueOf(jwt.getSubject()), request);
        return ResponseEntity.noContent().build();
    }
}
