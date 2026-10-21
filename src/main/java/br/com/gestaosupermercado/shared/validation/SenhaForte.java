package br.com.gestaosupermercado.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Política de senha única para todo o sistema: 8 a 72 caracteres com ao menos uma letra e um número.
 * O teto de 72 vem do BCrypt, que ignora silenciosamente os bytes excedentes.
 *
 * @since 0.0.1
 */
@Documented
@Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).{8,72}$")
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SenhaForte {

    String message() default "a senha deve ter de 8 a 72 caracteres, com ao menos uma letra e um número";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
