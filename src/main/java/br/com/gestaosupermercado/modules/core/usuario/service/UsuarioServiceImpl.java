package br.com.gestaosupermercado.modules.core.usuario.service;

import br.com.gestaosupermercado.modules.core.auth.service.SessaoService;
import br.com.gestaosupermercado.modules.core.perfil.dto.response.PerfilResumoResponse;
import br.com.gestaosupermercado.modules.core.perfil.model.Perfil;
import br.com.gestaosupermercado.modules.core.perfil.service.PerfilService;
import br.com.gestaosupermercado.modules.core.usuario.dto.request.SenhaAlteracaoRequest;
import br.com.gestaosupermercado.modules.core.usuario.dto.request.UsuarioCreateRequest;
import br.com.gestaosupermercado.modules.core.usuario.dto.request.UsuarioFiltroRequest;
import br.com.gestaosupermercado.modules.core.usuario.dto.request.UsuarioUpdateRequest;
import br.com.gestaosupermercado.modules.core.usuario.dto.response.UsuarioLogadoResponse;
import br.com.gestaosupermercado.modules.core.usuario.dto.response.UsuarioResponse;
import br.com.gestaosupermercado.modules.core.usuario.model.Usuario;
import br.com.gestaosupermercado.modules.core.usuario.repository.UsuarioRepository;
import br.com.gestaosupermercado.modules.core.usuario.repository.specs.UsuarioSpecs;
import br.com.gestaosupermercado.shared.dto.PaginaResponse;
import br.com.gestaosupermercado.shared.exception.BusinessException;
import br.com.gestaosupermercado.shared.exception.MensagemErro;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão de usuários e da própria conta.
 * <p>
 * Invariante protegida aqui: o sistema nunca fica sem ao menos um usuário ativo com o perfil do sistema
 * (Administrador). Inativar ou trocar os perfis do último Administrador é recusado.
 *
 * @since 0.0.1
 */
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of("nome", "email", "ativo", "criadoEm", "ultimoLoginEm");

    private final UsuarioRepository usuarioRepository;
    private final PerfilService perfilService;
    private final SessaoService sessaoService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> findAllUsuarios(final UsuarioFiltroRequest filtro, final Pageable pageable) {
        return PaginaResponse.of(usuarioRepository.findAll(UsuarioSpecs.byFiltro(filtro), toPageableSeguro(pageable))
                .map(this::toUsuarioResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse findUsuarioById(final Long id) {
        return toUsuarioResponse(findUsuarioEntityById(id));
    }

    @Override
    @Transactional
    public UsuarioResponse saveUsuario(final UsuarioCreateRequest request) {
        final String email = normalizeEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw BusinessException.conflict(MensagemErro.EMAIL_JA_CADASTRADO);
        }
        final Set<Perfil> perfis = perfilService.findPerfisAtivosForVinculo(request.perfilIds());
        final Usuario usuario = new Usuario(request.nome().strip(), email, passwordEncoder.encode(request.senha()), perfis,
                OffsetDateTime.now(clock));
        return toUsuarioResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    public UsuarioResponse updateUsuario(final Long id, final UsuarioUpdateRequest request) {
        final Usuario usuario = findUsuarioEntityById(id);
        final String email = normalizeEmail(request.email());
        if (usuarioRepository.existsByEmailAndIdNot(email, id)) {
            throw BusinessException.conflict(MensagemErro.EMAIL_JA_CADASTRADO);
        }
        final Set<Perfil> perfis = perfilService.findPerfisAtivosForVinculo(request.perfilIds());
        if (isAdministradorAtivo(usuario) && perfis.stream().noneMatch(Perfil::isSistema)) {
            ensureOutroAdministradorAtivo(id);
        }
        usuario.setNome(request.nome().strip());
        usuario.setEmail(email);
        usuario.replacePerfis(perfis);
        return toUsuarioResponse(usuario);
    }

    @Override
    @Transactional
    public void updateStatusUsuario(final Long id, final boolean ativo, final Long usuarioLogadoId) {
        if (id.equals(usuarioLogadoId)) {
            throw BusinessException.unprocessable(MensagemErro.USUARIO_NAO_PODE_ALTERAR_PROPRIO_STATUS);
        }
        final Usuario usuario = findUsuarioEntityById(id);
        if (!ativo && isAdministradorAtivo(usuario)) {
            ensureOutroAdministradorAtivo(id);
        }
        usuario.setAtivo(ativo);
        if (!ativo) {
            sessaoService.revokeAllSessoes(id);
        }
    }

    @Override
    @Transactional
    public void updateSenhaUsuario(final Long id, final String novaSenha) {
        final Usuario usuario = findUsuarioEntityById(id);
        usuario.updateSenha(passwordEncoder.encode(novaSenha), OffsetDateTime.now(clock));
        sessaoService.revokeAllSessoes(id);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioLogadoResponse findUsuarioLogado(final Long usuarioLogadoId) {
        final Usuario usuario = findUsuarioEntityById(usuarioLogadoId);
        return new UsuarioLogadoResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), toPerfilResumoResponses(usuario),
                usuarioRepository.findPermissoesEfetivasByUsuarioId(usuarioLogadoId).stream().sorted().toList());
    }

    /**
     * Encerra as outras sessões do usuário; o cliente atual precisa fazer login de novo com a nova senha.
     */
    @Override
    @Transactional
    public void updateSenhaUsuarioLogado(final Long usuarioLogadoId, final SenhaAlteracaoRequest request) {
        final Usuario usuario = findUsuarioEntityById(usuarioLogadoId);
        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
            throw BusinessException.unprocessable(MensagemErro.SENHA_ATUAL_INCORRETA);
        }
        if (passwordEncoder.matches(request.novaSenha(), usuario.getSenhaHash())) {
            throw BusinessException.unprocessable(MensagemErro.NOVA_SENHA_IGUAL_ATUAL);
        }
        usuario.updateSenha(passwordEncoder.encode(request.novaSenha()), OffsetDateTime.now(clock));
        sessaoService.revokeAllSessoes(usuarioLogadoId);
    }

    private Usuario findUsuarioEntityById(final Long id) {
        return usuarioRepository.findWithPerfisById(id)
                .orElseThrow(() -> BusinessException.notFound(MensagemErro.USUARIO_NAO_ENCONTRADO));
    }

    private static boolean isAdministradorAtivo(final Usuario usuario) {
        return usuario.isAtivo() && usuario.getPerfis().stream().anyMatch(perfil -> perfil.isSistema() && perfil.isAtivo());
    }

    private void ensureOutroAdministradorAtivo(final Long usuarioId) {
        if (usuarioRepository.countAdministradoresAtivosExcept(usuarioId) == 0) {
            throw BusinessException.unprocessable(MensagemErro.ULTIMO_ADMINISTRADOR);
        }
    }

    /**
     * Ordenação só por campos de lista branca: ordenar por {@code senhaHash} ou campos de bloqueio permitiria
     * inferir dados sensíveis pela posição dos registros.
     */
    private static Pageable toPageableSeguro(final Pageable pageable) {
        final Sort sort = Sort.by(pageable.getSort().stream()
                .filter(order -> CAMPOS_ORDENACAO.contains(order.getProperty()))
                .toList());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.isSorted() ? sort : Sort.by("nome"));
    }

    private static String normalizeEmail(final String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private UsuarioResponse toUsuarioResponse(final Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.isAtivo(),
                usuario.getUltimoLoginEm(), usuario.getCriadoEm(), toPerfilResumoResponses(usuario));
    }

    private static List<PerfilResumoResponse> toPerfilResumoResponses(final Usuario usuario) {
        return usuario.getPerfis().stream()
                .sorted(Comparator.comparing(Perfil::getNome))
                .map(perfil -> new PerfilResumoResponse(perfil.getId(), perfil.getNome(), perfil.isAtivo()))
                .toList();
    }
}
