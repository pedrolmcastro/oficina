package com.oficina.api.dominio.ordemservico;

import com.oficina.api.dominio.DadoInvalidoException;
import com.oficina.api.dominio.Entidade;
import com.oficina.api.dominio.Quantidade;
import com.oficina.api.dominio.insumo.Insumo;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "ordens_servico", schema = "oficina")
public class OrdemServico extends Entidade {
    private static final String EXECUCAO_CONCLUIDA = "EXECUCAO_CONCLUIDA";
    private static final String ITEM_ID = "itemId";
    private static final String ORCAMENTO_REJEITADO = "ORCAMENTO_REJEITADO";
    private UUID clienteId;
    private UUID veiculoId;
    @Enumerated(value = EnumType.STRING)
    private StatusOrdemServico status;
    private Instant criadaEm;
    private @Nullable String resultado;
    private String hashToken;
    private Instant tokenExpiraEm;
    private @Nullable String hashTokenDecisao;
    private @Nullable Instant decisaoExpiraEm;
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "ordem_servico_id", nullable = false, updatable = false)
    @OrderBy(value = "id ASC")
    private List<ServicoContratado> servicos = new ArrayList<>();
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ordem_servico_id", nullable = false, updatable = false)
    @OrderBy(value = "id ASC")
    private List<InsumoNecessario> insumos = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "ordens_servico_eventos", schema = "oficina", joinColumns = {
        @JoinColumn(name = "ordem_servico_id", nullable = false) })
    @OrderBy(value = "sequencia ASC")
    private List<EventoOrdemServico> eventos = new ArrayList<>();

    @SuppressWarnings(value = "NullAway.Init") // JPA preenche os campos após chamar este construtor
    protected OrdemServico() {}

    private OrdemServico(
        final UUID clienteId,
        final UUID veiculoId,
        final List<ServicoContratado> servicos,
        final String hashToken,
        final Instant tokenExpiraEm,
        final String autor
    ) {
        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.servicos.addAll(servicos);
        this.criadaEm = Instant.now();
        this.hashToken = hashToken;
        this.tokenExpiraEm = tokenExpiraEm;
        this.status = servicos.stream().anyMatch(servico -> servico.requerDiagnostico())
            ? StatusOrdemServico.AGUARDANDO_DIAGNOSTICO
            : StatusOrdemServico.AGUARDANDO_CONFIRMACAO;
        registrar("CRIADA", autor, Map.of());
        if (this.status == StatusOrdemServico.AGUARDANDO_CONFIRMACAO) {
            registrar("DIAGNOSTICO_DISPENSADO", "SISTEMA", Map.of());
        }
    }

    public static OrdemServico criar(
        final UUID clienteId,
        final UUID veiculoId,
        final List<ServicoContratado> servicos,
        final String hashToken,
        final Instant tokenExpiraEm,
        final String autor
    ) {
        if (servicos.isEmpty()
            || servicos.stream().map(servico -> servico.servicoId()).distinct().count() != servicos.size()) {
            throw new DadoInvalidoException("Informe ao menos um serviço, sem duplicatas");
        }
        return new OrdemServico(clienteId, veiculoId, servicos, hashToken, tokenExpiraEm, autor);
    }

    public UUID clienteId() {
        return this.clienteId;
    }

    public UUID veiculoId() {
        return this.veiculoId;
    }

    public StatusOrdemServico status() {
        return this.status;
    }

    public Instant criadaEm() {
        return this.criadaEm;
    }

    public @Nullable String resultado() {
        return this.resultado;
    }

    public String hashToken() {
        return this.hashToken;
    }

    public Instant tokenExpiraEm() {
        return this.tokenExpiraEm;
    }

    public @Nullable String hashTokenDecisao() {
        return this.hashTokenDecisao;
    }

    public @Nullable Instant decisaoExpiraEm() {
        return this.decisaoExpiraEm;
    }

    public List<ServicoContratado> servicos() {
        return List.copyOf(this.servicos);
    }

    public List<InsumoNecessario> insumos() {
        return List.copyOf(this.insumos);
    }

    public List<EventoOrdemServico> eventos() {
        return List.copyOf(this.eventos);
    }

    public BigDecimal total() {
        return this.servicos
            .stream()
            .map(servico -> servico.preco().preco().valor())
            .reduce(BigDecimal.ZERO, (total, preco) -> total.add(preco))
            .add(
                this.insumos
                    .stream()
                    .map(insumo -> insumo.subtotal())
                    .reduce(BigDecimal.ZERO, (total, subtotal) -> total.add(subtotal))
            );
    }

    public void associarObrigatorio(final Insumo insumo, final UUID servicoContratadoId, final Quantidade quantidade) {
        if (this.servicos.stream().noneMatch(s -> s.id().equals(servicoContratadoId))) {
            throw new DadoInvalidoException("Serviço não pertence à OS");
        }
        this.insumos.add(new InsumoNecessario(insumo, servicoContratadoId, quantidade));
    }

    public InsumoNecessario adicionarInsumo(final Insumo insumo, final Quantidade quantidade, final String autor) {
        exigirStatus(StatusOrdemServico.EM_DIAGNOSTICO);
        if (this.insumos.stream().anyMatch(i -> i.avulso() && i.insumoId().equals(insumo.id()))) {
            throw new ConflitoOrdemServicoException("Insumo avulso já incluído na OS; atualize sua quantidade");
        }
        final var item = new InsumoNecessario(insumo, null, quantidade);
        this.insumos.add(item);
        registrar(
            "INSUMO_AVULSO_ADICIONADO",
            autor,
            Map
                .of(
                    ITEM_ID,
                    item.id().toString(),
                    "insumoId",
                    insumo.id().toString(),
                    "quantidade",
                    Integer.toString(quantidade.valor())
                )
        );
        return item;
    }

    public InsumoNecessario setQuantidade(final UUID itemId, final Quantidade quantidade, final String autor) {
        exigirStatus(StatusOrdemServico.EM_DIAGNOSTICO);
        final var item = encontrarInsumo(itemId);
        final var anterior = item.quantidade();
        item.setQuantidade(quantidade);
        if (anterior.equals(quantidade)) {
            return item;
        }
        registrar(
            "INSUMO_AVULSO_ALTERADO",
            autor,
            Map
                .of(
                    ITEM_ID,
                    itemId.toString(),
                    "anterior",
                    Integer.toString(anterior.valor()),
                    "quantidade",
                    Integer.toString(quantidade.valor())
                )
        );
        return item;
    }

    public void removerInsumo(final UUID itemId, final String autor) {
        exigirStatus(StatusOrdemServico.EM_DIAGNOSTICO);
        final var item = encontrarInsumo(itemId);
        item.exigirAvulso();
        this.insumos.remove(item);
        registrar("INSUMO_AVULSO_REMOVIDO", autor, Map.of(ITEM_ID, itemId.toString()));
    }

    public void iniciarDiagnostico(final String autor) {
        transicionar(
            StatusOrdemServico.AGUARDANDO_DIAGNOSTICO,
            StatusOrdemServico.EM_DIAGNOSTICO,
            "DIAGNOSTICO_INICIADO",
            autor
        );
    }

    public void concluirDiagnostico(final String autor) {
        transicionar(
            StatusOrdemServico.EM_DIAGNOSTICO,
            StatusOrdemServico.AGUARDANDO_CONFIRMACAO,
            "DIAGNOSTICO_CONCLUIDO",
            autor
        );
    }

    public void aprovar() {
        if (this.status == StatusOrdemServico.AGUARDANDO_EXECUCAO) {
            return;
        }
        transicionar(
            StatusOrdemServico.AGUARDANDO_CONFIRMACAO,
            StatusOrdemServico.AGUARDANDO_EXECUCAO,
            "ORCAMENTO_APROVADO",
            "CLIENTE"
        );
    }

    public void rejeitar() {
        if (ORCAMENTO_REJEITADO.equals(this.resultado)) {
            return;
        }
        transicionar(
            StatusOrdemServico.AGUARDANDO_CONFIRMACAO,
            StatusOrdemServico.AGUARDANDO_RETIRADA,
            ORCAMENTO_REJEITADO,
            "CLIENTE"
        );
        this.resultado = ORCAMENTO_REJEITADO;
    }

    public void iniciarExecucao(final String autor) {
        transicionar(
            StatusOrdemServico.AGUARDANDO_EXECUCAO,
            StatusOrdemServico.EM_EXECUCAO,
            "EXECUCAO_INICIADA",
            autor
        );
    }

    public void concluirExecucao(final String autor) {
        transicionar(StatusOrdemServico.EM_EXECUCAO, StatusOrdemServico.AGUARDANDO_RETIRADA, EXECUCAO_CONCLUIDA, autor);
        this.resultado = EXECUCAO_CONCLUIDA;
    }

    public void retirarVeiculo(final String autor) {
        transicionar(StatusOrdemServico.AGUARDANDO_RETIRADA, StatusOrdemServico.ENTREGUE, "VEICULO_RETIRADO", autor);
    }

    public void emitirAcessoDecisao(final String hash, final Instant expiraEm) {
        exigirStatus(StatusOrdemServico.AGUARDANDO_CONFIRMACAO);
        this.hashTokenDecisao = hash;
        this.decisaoExpiraEm = expiraEm;
    }

    public void renovarAcesso(final String hash, final Instant expiraEm, final String autor) {
        this.hashTokenDecisao = null;
        this.decisaoExpiraEm = null;
        this.hashToken = hash;
        this.tokenExpiraEm = expiraEm;
        registrar("ACESSO_REEMITIDO", autor, Map.of());
    }

    private InsumoNecessario encontrarInsumo(final UUID itemId) {
        return this.insumos
            .stream()
            .filter(i -> i.id().equals(itemId))
            .findFirst()
            .orElseThrow(InsumoNecessarioNaoEncontradoException::new);
    }

    private void transicionar(
        final StatusOrdemServico esperado,
        final StatusOrdemServico destino,
        final String evento,
        final String autor
    ) {
        exigirStatus(esperado);
        this.status = destino;
        registrar(evento, autor, Map.of("total", total().toPlainString()));
    }

    private void exigirStatus(final StatusOrdemServico esperado) {
        if (this.status != esperado) {
            throw new ConflitoOrdemServicoException("Operação exige OS em " + esperado.valor());
        }
    }

    private void registrar(final String tipo, final String autor, final Map<String, String> dados) {
        this.eventos.add(new EventoOrdemServico(this.eventos.size(), tipo, Instant.now(), autor, dados));
    }
}
