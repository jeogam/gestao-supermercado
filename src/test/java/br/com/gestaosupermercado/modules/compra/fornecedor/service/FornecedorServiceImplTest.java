package br.com.gestaosupermercado.modules.compra.fornecedor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.gestaosupermercado.modules.compra.fornecedor.dto.request.FornecedorRequest;
import br.com.gestaosupermercado.modules.compra.fornecedor.dto.response.FornecedorResponse;
import br.com.gestaosupermercado.modules.compra.fornecedor.model.Fornecedor;
import br.com.gestaosupermercado.modules.compra.fornecedor.repository.FornecedorRepository;
import br.com.gestaosupermercado.shared.exception.ErrorMessage;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FornecedorServiceImplTest {

    @Mock
    private FornecedorRepository fornecedorRepository;

    private FornecedorServiceImpl fornecedorService;

    @BeforeEach
    void setUp() {
        fornecedorService = new FornecedorServiceImpl(fornecedorRepository);
    }

    @Test
    void shouldNormalizeCnpjAndEmail() {
        when(fornecedorRepository.save(any())).then(AdditionalAnswers.returnsFirstArg());

        final FornecedorResponse resposta = fornecedorService.saveFornecedor(
                new FornecedorRequest("  Atacadão  ", "12.345.678/0001-90", " ", " Vendas@Atacadao.com "));

        assertThat(resposta.nome()).isEqualTo("Atacadão");
        assertThat(resposta.cnpj()).isEqualTo("12345678000190");
        assertThat(resposta.telefone()).isNull();
        assertThat(resposta.email()).isEqualTo("vendas@atacadao.com");
    }

    @Test
    void shouldRejectCnpjJaCadastrado() {
        when(fornecedorRepository.existsByCnpj("12345678000190")).thenReturn(true);

        assertThatThrownBy(() -> fornecedorService.saveFornecedor(new FornecedorRequest("Atacadão", "12345678000190", null, null)))
                .hasMessage(ErrorMessage.CNPJ_JA_CADASTRADO.getDescricao());
        verify(fornecedorRepository, never()).save(any());
    }

    @Test
    void shouldRejectFornecedorInativoForCompra() {
        final Fornecedor fornecedor = new Fornecedor("Atacadão", null, null, null);
        fornecedor.setAtivo(false);
        when(fornecedorRepository.findById(4L)).thenReturn(Optional.of(fornecedor));

        assertThatThrownBy(() -> fornecedorService.findFornecedorAtivoForCompra(4L))
                .hasMessage(ErrorMessage.FORNECEDOR_INATIVO.getDescricao());
    }
}
