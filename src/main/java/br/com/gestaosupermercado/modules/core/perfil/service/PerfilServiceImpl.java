package br.com.gestaosupermercado.modules.core.perfil.service;

import br.com.gestaosupermercado.modules.core.perfil.dto.request.PerfilRequest;
import br.com.gestaosupermercado.modules.core.perfil.dto.response.PerfilResponse;
import br.com.gestaosupermercado.modules.core.perfil.model.Perfil;
import br.com.gestaosupermercado.modules.core.perfil.repository.PerfilRepository;
import br.com.gestaosupermercado.modules.core.permissao.enums.Permissao;
import br.com.gestaosupermercado.shared.exception.BusinessException;
import br.com.gestaosupermercado.shared.exception.MensagemErro;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PerfilServiceImpl implements PerfilService {

    private final PerfilRepository perfilRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PerfilResponse> findAllPerfis(final Boolean ativo) {
        return perfilRepository.findAllPerfisByAtivo(ativo).stream().map(this::toPerfilResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PerfilResponse findPerfilById(final Long id) {
        return toPerfilResponse(findPerfilEntityById(id));
    }

    @Override
    @Transactional
    public PerfilResponse savePerfil(final PerfilRequest request) {
        final String nome = request.nome().strip();
        if (perfilRepository.existsByNomeIgnoreCase(nome)) {
            throw BusinessException.conflict(MensagemErro.PERFIL_JA_CADASTRADO);
        }
        final Perfil perfil = new Perfil(nome, request.descricao(), request.permissoes());
        return toPerfilResponse(perfilRepository.save(perfil));
    }

    @Override
    @Transactional
    public PerfilResponse updatePerfil(final Long id, final PerfilRequest request) {
        final Perfil perfil = findPerfilEditavelById(id);
        final String nome = request.nome().strip();
        if (perfilRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw BusinessException.conflict(MensagemErro.PERFIL_JA_CADASTRADO);
        }
        perfil.setNome(nome);
        perfil.setDescricao(request.descricao());
        perfil.replacePermissoes(request.permissoes());
        return toPerfilResponse(perfil);
    }

    @Override
    @Transactional
    public void updateStatusPerfil(final Long id, final boolean ativo) {
        findPerfilEditavelById(id).setAtivo(ativo);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Perfil> findPerfisAtivosForVinculo(final Set<Long> ids) {
        final List<Perfil> perfis = perfilRepository.findAllByIdIn(ids);
        if (perfis.size() != ids.size()) {
            throw BusinessException.notFound(MensagemErro.PERFIL_NAO_ENCONTRADO);
        }
        if (perfis.stream().anyMatch(perfil -> !perfil.isAtivo())) {
            throw BusinessException.unprocessable(MensagemErro.PERFIL_INATIVO);
        }
        return new HashSet<>(perfis);
    }

    private Perfil findPerfilEntityById(final Long id) {
        return perfilRepository.findWithPermissoesById(id)
                .orElseThrow(() -> BusinessException.notFound(MensagemErro.PERFIL_NAO_ENCONTRADO));
    }

    /**
     * O perfil do sistema (Superadmin) é imutável pela API: editá-lo ou inativá-lo poderia deixar a loja
     * sem ninguém capaz de gerir acessos.
     */
    private Perfil findPerfilEditavelById(final Long id) {
        final Perfil perfil = findPerfilEntityById(id);
        if (perfil.isSistema()) {
            throw BusinessException.unprocessable(MensagemErro.PERFIL_DO_SISTEMA_NAO_EDITAVEL);
        }
        return perfil;
    }

    private PerfilResponse toPerfilResponse(final Perfil perfil) {
        // Superadmin tem todas as permissões por código (ver PermissaoEfetivaService); a tela mostra isso.
        final List<Permissao> permissoes = perfil.isSistema()
                ? List.of(Permissao.values())
                : perfil.getPermissoes().stream().sorted().toList();
        return new PerfilResponse(perfil.getId(), perfil.getNome(), perfil.getDescricao(), perfil.isAtivo(), perfil.isSistema(),
                permissoes);
    }
}
