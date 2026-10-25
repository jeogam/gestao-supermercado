package br.com.gestaosupermercado.shared.exception;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Converte exceções em {@link ErroResponse}. Erros inesperados nunca expõem detalhes internos ao cliente;
 * o stack trace fica apenas no log.
 *
 * @since 0.0.1
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErroResponse> handleBusinessException(final BusinessException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ErroResponse.of(exception.getStatus().value(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidationException(final MethodArgumentNotValidException exception) {
        final List<ErroResponse.CampoErro> campos = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ErroResponse.CampoErro(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        final int status = HttpStatus.UNPROCESSABLE_CONTENT.value();
        return ResponseEntity.status(status)
                .body(new ErroResponse(status, "Dados inválidos.", campos, OffsetDateTime.now()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> handleNotReadableException(final HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(ErroResponse.of(HttpStatus.BAD_REQUEST.value(), "Corpo da requisição inválido."));
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErroResponse> handleAccessDeniedException(final RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErroResponse.of(HttpStatus.FORBIDDEN.value(), "Você não tem permissão para esta operação."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> handleNoResourceFoundException(final NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErroResponse.of(HttpStatus.NOT_FOUND.value(), "Recurso não encontrado."));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErroResponse> handleOptimisticLockingException(final ObjectOptimisticLockingFailureException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErroResponse.of(HttpStatus.CONFLICT.value(), "O registro foi alterado por outra operação. Tente novamente."));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErroResponse> handleParametroInvalidoException(final Exception exception) {
        return ResponseEntity.badRequest()
                .body(ErroResponse.of(HttpStatus.BAD_REQUEST.value(), "Parâmetro da requisição ausente ou inválido."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> handleMethodNotSupportedException(final HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErroResponse.of(HttpStatus.METHOD_NOT_ALLOWED.value(), "Método HTTP não suportado neste endereço."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> handleMediaTypeNotSupportedException(final HttpMediaTypeNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErroResponse.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "Envie o corpo como application/json."));
    }

    /**
     * Os services checam unicidade antes de gravar; isto só acontece quando duas requisições iguais chegam juntas
     * e a constraint do banco barra a segunda.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> handleDataIntegrityViolationException(final DataIntegrityViolationException exception) {
        log.warn("Violação de integridade: {}", exception.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErroResponse.of(HttpStatus.CONFLICT.value(), "O registro conflita com outro já existente. Atualize os dados e tente novamente."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleUnexpectedException(final Exception exception) {
        log.error("Erro inesperado", exception);
        return ResponseEntity.internalServerError()
                .body(ErroResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erro interno. Tente novamente mais tarde."));
    }
}
