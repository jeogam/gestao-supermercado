package br.com.gestaosupermercado.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Violação de regra de negócio. O status HTTP viaja com a exceção para que o service decida a semântica
 * (404, 409, 422...) sem depender do controller.
 *
 * @since 0.0.1
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(final MensagemErro mensagemErro, final HttpStatus status) {
        super(mensagemErro.getDescricao());
        this.status = status;
    }

    public static BusinessException notFound(final MensagemErro mensagemErro) {
        return new BusinessException(mensagemErro, HttpStatus.NOT_FOUND);
    }

    public static BusinessException conflict(final MensagemErro mensagemErro) {
        return new BusinessException(mensagemErro, HttpStatus.CONFLICT);
    }

    public static BusinessException unprocessable(final MensagemErro mensagemErro) {
        return new BusinessException(mensagemErro, HttpStatus.UNPROCESSABLE_CONTENT);
    }

    public static BusinessException unauthorized(final MensagemErro mensagemErro) {
        return new BusinessException(mensagemErro, HttpStatus.UNAUTHORIZED);
    }
}
