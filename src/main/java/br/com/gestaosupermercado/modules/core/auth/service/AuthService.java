package br.com.gestaosupermercado.modules.core.auth.service;

import br.com.gestaosupermercado.modules.core.auth.dto.request.CadastroRequest;
import br.com.gestaosupermercado.modules.core.auth.dto.request.LoginRequest;
import br.com.gestaosupermercado.modules.core.auth.dto.response.TokenResponse;

public interface AuthService {

    void cadastrar(CadastroRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(String refreshToken);

    void logout(String refreshToken);
}
