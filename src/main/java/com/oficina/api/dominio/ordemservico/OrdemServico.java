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
    private static final String INSUMO_NECESSARIO_ID = "insumoNecessarioId";
    private static final String ORCAMENTO_REJEITADO = "ORCAMENTO_REJEITADO";
    private static final String SISTEMA = "SISTEMA";
    private static final String QUANTIDADE = "quantidade";
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
    private List<ServicoContratado> servicosContratados = new ArrayList<>();
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ordem_servico_id", nullable = false, updatable = false)
    @OrderBy(value = "id ASC")
    private List<InsumoNecessario> insumosNecessarios = new ArrayList<>();
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
        final List<ServicoContratado> servicosContratados,
        final String hashToken,
        final Instant tokenExpiraEm,
        final String autor
    ) {
        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.servicosContratados.addAll(servicosContratados);
        this.criadaEm = Instant.now();
        this.hashToken = hashToken;
        this.tokenExpiraEm = tokenExpiraEm;
        this.status = servicosContratados.stream().anyMatch(servico -> servico.requerDiagnostico())
            ? StatusOrdemServico.AGUARDANDO_DIAGNOSTICO
            : StatusOrdemServico.AGUARDANDO_CONFIRMACAO;
        registrar("CRIADA", autor, Map.of());
        if (this.status == StatusOrdemServico.AGUARDANDO_CONFIRMACAO) {
            registrar("DIAGNOSTICO_DISPENSADO", SISTEMA, Map.of());
        }
    }

    public static OrdemServico criar(
        final UUID clienteId,
        final UUID veiculoId,
        final List<ServicoContratado> servicosContratados,
        final String hashToken,
        final Instant tokenExpiraEm,
        final String autor
    ) {
        if (servicosContratados.isEmpty()
            || servicosContratados
                .stream()
                .map(servico -> servico.servicoId())
                .distinct()
                .count() != servicosContratados.size()) {
            throw new DadoInvalidoException("Informe ao menos um serviço, sem duplicatas");
        }
        return new OrdemServico(clienteId, veiculoId, servicosContratados, hashToken, tokenExpiraEm, autor);
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

    public List<ServicoContratado> servicosContratados() {
        return List.copyOf(this.servicosContratados);
    }

    public List<InsumoNecessario> insumosNecessarios() {
        return List.copyOf(this.insumosNecessarios);
    }

    public List<EventoOrdemServico> eventos() {
        return List.copyOf(this.eventos);
    }

    public BigDecimal orcamento() {
        return this.servicosContratados
            .stream()
            .map(servico -> servico.precoVenda().preco().valor())
            .reduce(BigDecimal.ZERO, (orcamento, preco) -> orcamento.add(preco))
            .add(
                this.insumosNecessarios
                    .stream()
                    .map(insumo -> insumo.subtotal())
                    .reduce(BigDecimal.ZERO, (orcamento, subtotal) -> orcamento.add(subtotal))
            );
    }

    public void associarInsumoObrigatorio(
        final Insumo insumo,
        final UUID servicoContratadoId,
        final Quantidade quantidade
    ) {
        if (this.servicosContratados.stream().noneMatch(s -> s.id().equals(servicoContratadoId))) {
            throw new DadoInvalidoException("Serviço não pertence à OS");
        }
        this.insumosNecessarios.add(new InsumoNecessario(insumo, servicoContratadoId, quantidade));
    }

    public InsumoNecessario adicionarInsumoAvulso(
        final Insumo insumo,
        final Quantidade quantidade,
        final String autor
    ) {
        exigirStatus(StatusOrdemServico.EM_DIAGNOSTICO);
        if (this.insumosNecessarios.stream().anyMatch(i -> i.avulso() && i.insumoId().equals(insumo.id()))) {
            throw new ConflitoOrdemServicoException("Insumo avulso já incluído na OS; atualize sua quantidade");
        }
        final var insumoNecessario = new InsumoNecessario(insumo, null, quantidade);
        this.insumosNecessarios.add(insumoNecessario);
        registrar(
            "INSUMO_AVULSO_ADICIONADO",
            autor,
            Map
                .of(
                    INSUMO_NECESSARIO_ID,
                    insumoNecessario.id().toString(),
                    "insumoId",
                    insumo.id().toString(),
                    QUANTIDADE,
                    Integer.toString(quantidade.valor())
                )
        );
        return insumoNecessario;
    }

    public InsumoNecessario alterarQuantidadeInsumoAvulso(
        final UUID insumoNecessarioId,
        final Quantidade quantidade,
        final String autor
    ) {
        exigirStatus(StatusOrdemServico.EM_DIAGNOSTICO);
        final var insumoNecessario = encontrarInsumo(insumoNecessarioId);
        final var anterior = insumoNecessario.quantidade();
        insumoNecessario.setQuantidade(quantidade);
        if (anterior.equals(quantidade)) {
            return insumoNecessario;
        }
        registrar(
            "INSUMO_AVULSO_ALTERADO",
            autor,
            Map
                .of(
                    INSUMO_NECESSARIO_ID,
                    insumoNecessarioId.toString(),
                    "anterior",
                    Integer.toString(anterior.valor()),
                    QUANTIDADE,
                    Integer.toString(quantidade.valor())
                )
        );
        return insumoNecessario;
    }

    public void removerInsumoAvulso(final UUID insumoNecessarioId, final String autor) {
        exigirStatus(StatusOrdemServico.EM_DIAGNOSTICO);
        final var insumoNecessario = encontrarInsumo(insumoNecessarioId);
        insumoNecessario.exigirAvulso();
        this.insumosNecessarios.remove(insumoNecessario);
        registrar("INSUMO_AVULSO_REMOVIDO", autor, Map.of(INSUMO_NECESSARIO_ID, insumoNecessarioId.toString()));
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
        if (this.eventos.stream().anyMatch(evento -> "ORCAMENTO_APROVADO".equals(evento.tipo()))) {
            return;
        }
        transicionar(
            StatusOrdemServico.AGUARDANDO_CONFIRMACAO,
            this.insumosNecessarios.isEmpty()
                ? StatusOrdemServico.AGUARDANDO_EXECUCAO
                : StatusOrdemServico.AGUARDANDO_INSUMOS,
            "ORCAMENTO_APROVADO",
            "CLIENTE"
        );
    }

    public void reservarInsumo(final UUID insumoNecessarioId, final Quantidade quantidade) {
        exigirStatus(StatusOrdemServico.AGUARDANDO_INSUMOS);
        final var insumoNecessario = encontrarInsumo(insumoNecessarioId);
        insumoNecessario.reservar(quantidade);
        registrar(
            "INSUMO_RESERVADO",
            SISTEMA,
            Map
                .of(
                    INSUMO_NECESSARIO_ID,
                    insumoNecessarioId.toString(),
                    QUANTIDADE,
                    Integer.toString(quantidade.valor())
                )
        );
        if (this.insumosNecessarios.stream().allMatch(i -> i.quantidadePendente() == 0)) {
            transicionar(
                StatusOrdemServico.AGUARDANDO_INSUMOS,
                StatusOrdemServico.AGUARDANDO_EXECUCAO,
                "INSUMOS_RESERVADOS",
                SISTEMA
            );
        }
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

    public void consumirReservas() {
        exigirStatus(StatusOrdemServico.EM_EXECUCAO);
        for (final var insumoNecessario : this.insumosNecessarios) {
            insumoNecessario.consumirReserva();
        }
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

    private InsumoNecessario encontrarInsumo(final UUID insumoNecessarioId) {
        return this.insumosNecessarios
            .stream()
            .filter(i -> i.id().equals(insumoNecessarioId))
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
        registrar(evento, autor, Map.of("orcamento", orcamento().toPlainString()));
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
