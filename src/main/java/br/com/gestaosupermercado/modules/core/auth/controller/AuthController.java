package br.com.gestaosupermercado.modules.core.auth.controller;

import br.com.gestaosupermercado.modules.core.auth.dto.request.LoginRequest;
import br.com.gestaosupermercado.modules.core.auth.dto.request.RefreshTokenRequest;
import br.com.gestaosupermercado.modules.core.auth.dto.response.TokenResponse;
import br.com.gestaosupermercado.modules.core.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação")
@SecurityRequirements
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Autentica com e-mail e senha e abre uma sessão")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas, usuário inativo ou bloqueado"),
            @ApiResponse(responseCode = "422", description = "Dados inválidos")
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid final LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Renova a sessão; o refresh token enviado deixa de valer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessão renovada"),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido, expirado ou já utilizado")
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody @Valid final RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @Operation(summary = "Encerra a sessão do refresh token informado")
    @ApiResponses(@ApiResponse(responseCode = "204", description = "Sessão encerrada (idempotente)"))
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid final RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
