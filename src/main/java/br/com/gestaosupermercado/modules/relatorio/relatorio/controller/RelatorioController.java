package br.com.gestaosupermercado.modules.relatorio.relatorio.controller;

import br.com.gestaosupermercado.modules.relatorio.relatorio.dto.request.RelatorioFiltroRequest;
import br.com.gestaosupermercado.modules.relatorio.relatorio.dto.response.FaturamentoResponse;
import br.com.gestaosupermercado.modules.relatorio.relatorio.dto.response.MargemProdutoResponse;
import br.com.gestaosupermercado.modules.relatorio.relatorio.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Relatórios")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/relatorio/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    @Operation(summary = "Faturamento, custo, lucro e margem do período, com o faturamento por forma de pagamento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relatório gerado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "422", description = "Período ausente ou inválido")
    })
    @GetMapping("/faturamento")
    @PreAuthorize("hasAuthority('RELATORIO_VISUALIZAR')")
    public ResponseEntity<FaturamentoResponse> findFaturamento(@ParameterObject @Valid final RelatorioFiltroRequest filtro) {
        return ResponseEntity.ok(relatorioService.findFaturamento(filtro));
    }

    @Operation(summary = "Margem por produto no período, do maior para o menor lucro")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relatório gerado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "422", description = "Período ausente ou inválido")
    })
    @GetMapping("/margem")
    @PreAuthorize("hasAuthority('RELATORIO_VISUALIZAR')")
    public ResponseEntity<List<MargemProdutoResponse>> findMargemProdutos(@ParameterObject @Valid final RelatorioFiltroRequest filtro) {
        return ResponseEntity.ok(relatorioService.findMargemProdutos(filtro));
    }

    @Operation(summary = "Relatório do período em PDF (faturamento e margem por produto)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF gerado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão"),
            @ApiResponse(responseCode = "422", description = "Período ausente ou inválido")
    })
    // Sem "produces": com ele, um erro de validação (JSON) não poderia ser escrito e viraria 406.
    @GetMapping("/pdf")
    @PreAuthorize("hasAuthority('RELATORIO_VISUALIZAR')")
    public ResponseEntity<byte[]> generateRelatorioPdf(@ParameterObject @Valid final RelatorioFiltroRequest filtro) {
        final String nomeArquivo = "relatorio-%s-a-%s.pdf".formatted(filtro.dataInicio().toLocalDate(), filtro.dataFim().toLocalDate());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nomeArquivo).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(relatorioService.generateRelatorioPdf(filtro));
    }
}
