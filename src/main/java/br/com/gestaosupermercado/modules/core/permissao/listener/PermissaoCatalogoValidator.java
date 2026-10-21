package br.com.gestaosupermercado.modules.core.permissao.listener;

import br.com.gestaosupermercado.modules.core.permissao.enums.Permissao;
import br.com.gestaosupermercado.modules.core.permissao.model.PermissaoCatalogo;
import br.com.gestaosupermercado.modules.core.permissao.repository.PermissaoCatalogoRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Garante que o enum {@link Permissao} e a tabela {@code permissao} estão sincronizados. Uma permissão
 * esquecida na migration faria o perfil nunca recebê-la; uma esquecida no enum quebraria a leitura de
 * {@code perfil_permissao}. Falhar no start é mais barato que descobrir isso em produção.
 *
 * @since 0.0.1
 */
@Component
@RequiredArgsConstructor
public class PermissaoCatalogoValidator {

    private final PermissaoCatalogoRepository permissaoCatalogoRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void validatePermissaoCatalogo() {
        final Set<String> codigosBanco = permissaoCatalogoRepository.findAll().stream()
                .map(PermissaoCatalogo::getCodigo)
                .collect(Collectors.toSet());
        final Set<String> codigosEnum = Arrays.stream(Permissao.values())
                .map(Permissao::name)
                .collect(Collectors.toSet());

        final List<String> faltandoNoBanco = codigosEnum.stream().filter(codigo -> !codigosBanco.contains(codigo)).sorted().toList();
        final List<String> faltandoNoEnum = codigosBanco.stream().filter(codigo -> !codigosEnum.contains(codigo)).sorted().toList();

        if (!faltandoNoBanco.isEmpty() || !faltandoNoEnum.isEmpty()) {
            throw new IllegalStateException("Catálogo de permissões dessincronizado. Faltando no banco: "
                    + faltandoNoBanco + "; faltando no enum Permissao: " + faltandoNoEnum);
        }
    }
}
