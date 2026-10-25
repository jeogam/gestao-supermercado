package br.com.gestaosupermercado.modules.compra.fornecedor.service;

import br.com.gestaosupermercado.modules.compra.fornecedor.dto.request.FornecedorFiltroRequest;
import br.com.gestaosupermercado.modules.compra.fornecedor.dto.request.FornecedorRequest;
import br.com.gestaosupermercado.modules.compra.fornecedor.dto.response.FornecedorResponse;
import br.com.gestaosupermercado.modules.compra.fornecedor.model.Fornecedor;
import br.com.gestaosupermercado.modules.compra.fornecedor.repository.FornecedorRepository;
import br.com.gestaosupermercado.modules.compra.fornecedor.repository.specs.FornecedorSpecs;
import br.com.gestaosupermercado.shared.dto.PaginaResponse;
import br.com.gestaosupermercado.shared.exception.BusinessException;
import br.com.gestaosupermercado.shared.exception.ErrorMessage;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro de fornecedores.
 *
 * @since 0.0.1
 */
@Service
@RequiredArgsConstructor
public class FornecedorServiceImpl implements FornecedorService {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of("nome", "cnpj", "ativo", "criadoEm");

    private final FornecedorRepository fornecedorRepository;

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<FornecedorResponse> findAllFornecedores(final FornecedorFiltroRequest filtro, final Pageable pageable) {
        return PaginaResponse.of(fornecedorRepository.findAll(FornecedorSpecs.byFiltro(filtro), toPageableSeguro(pageable))
                .map(FornecedorServiceImpl::toFornecedorResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public FornecedorResponse findFornecedorById(final Long id) {
        return toFornecedorResponse(findFornecedorEntityById(id));
    }

    @Override
    @Transactional
    public FornecedorResponse saveFornecedor(final FornecedorRequest request) {
        final String cnpj = normalizeCnpj(request.cnpj());
        if (cnpj != null && fornecedorRepository.existsByCnpj(cnpj)) {
            throw BusinessException.conflict(ErrorMessage.CNPJ_JA_CADASTRADO);
        }
        final Fornecedor fornecedor = new Fornecedor(request.nome().strip(), cnpj, normalizeOpcional(request.telefone()),
                normalizeEmail(request.email()));
        return toFornecedorResponse(fornecedorRepository.save(fornecedor));
    }

    @Override
    @Transactional
    public FornecedorResponse updateFornecedor(final Long id, final FornecedorRequest request) {
        final Fornecedor fornecedor = findFornecedorEntityById(id);
        final String cnpj = normalizeCnpj(request.cnpj());
        if (cnpj != null && fornecedorRepository.existsByCnpjAndIdNot(cnpj, id)) {
            throw BusinessException.conflict(ErrorMessage.CNPJ_JA_CADASTRADO);
        }
        fornecedor.setNome(request.nome().strip());
        fornecedor.setCnpj(cnpj);
        fornecedor.setTelefone(normalizeOpcional(request.telefone()));
        fornecedor.setEmail(normalizeEmail(request.email()));
        return toFornecedorResponse(fornecedor);
    }

    @Override
    @Transactional
    public void updateStatusFornecedor(final Long id, final boolean ativo) {
        findFornecedorEntityById(id).setAtivo(ativo);
    }

    @Override
    @Transactional(readOnly = true)
    public Fornecedor findFornecedorAtivoForCompra(final Long id) {
        final Fornecedor fornecedor = findFornecedorEntityById(id);
        if (!fornecedor.isAtivo()) {
            throw BusinessException.unprocessable(ErrorMessage.FORNECEDOR_INATIVO);
        }
        return fornecedor;
    }

    private Fornecedor findFornecedorEntityById(final Long id) {
        return fornecedorRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(ErrorMessage.FORNECEDOR_NAO_ENCONTRADO));
    }

    private static Pageable toPageableSeguro(final Pageable pageable) {
        final Sort sort = Sort.by(pageable.getSort().stream()
                .filter(order -> CAMPOS_ORDENACAO.contains(order.getProperty()))
                .toList());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.isSorted() ? sort : Sort.by("nome"));
    }

    // Guardado só com dígitos: "12.345.678/0001-90" e "12345678000190" são o mesmo fornecedor.
    private static String normalizeCnpj(final String cnpj) {
        return cnpj == null || cnpj.isBlank() ? null : cnpj.replaceAll("\\D", "");
    }

    private static String normalizeOpcional(final String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    private static String normalizeEmail(final String email) {
        final String valor = normalizeOpcional(email);
        return valor == null ? null : valor.toLowerCase(Locale.ROOT);
    }

    private static FornecedorResponse toFornecedorResponse(final Fornecedor fornecedor) {
        return new FornecedorResponse(fornecedor.getId(), fornecedor.getNome(), fornecedor.getCnpj(), fornecedor.getTelefone(),
                fornecedor.getEmail(), fornecedor.isAtivo(), fornecedor.getCriadoEm());
    }
}
