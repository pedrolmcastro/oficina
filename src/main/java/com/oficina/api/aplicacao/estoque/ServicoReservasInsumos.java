package com.oficina.api.aplicacao.estoque;

import com.oficina.api.aplicacao.insumo.InsumoNaoEncontradoException;
import com.oficina.api.dominio.Quantidade;
import com.oficina.api.dominio.insumo.Insumo;
import com.oficina.api.dominio.ordemservico.OrdemServico;
import com.oficina.api.infraestrutura.persistencia.insumo.RepositorioInsumos;
import com.oficina.api.infraestrutura.persistencia.ordemservico.RepositorioOrdensServico;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ServicoReservasInsumos {
    private final RepositorioInsumos insumos;
    private final RepositorioOrdensServico ordens;

    public ServicoReservasInsumos(final RepositorioInsumos insumos, final RepositorioOrdensServico ordens) {
        this.insumos = insumos;
        this.ordens = ordens;
    }

    // O chamador mantém o bloqueio até confirmar saldo, reservas e estado da OS
    // juntos.
    public void bloquear() {
        this.insumos.bloquearEstoque();
    }

    public void reservar(final OrdemServico ordem) {
        ordem.insumos().stream().map(i -> i.insumoId()).distinct().sorted().forEach(id -> distribuir(encontrar(id)));
    }

    public Insumo incrementar(final UUID insumoId, final Quantidade quantidade) {
        final var insumo = encontrar(insumoId);
        insumo.incrementarEstoque(quantidade);
        distribuir(insumo);
        return insumo;
    }

    public void consumir(final OrdemServico ordem) {
        for (final var item : ordem.insumos()) {
            encontrar(item.insumoId()).consumirReserva(new Quantidade(item.quantidadeReservada()));
        }
        ordem.consumirReservas();
    }

    private void distribuir(final Insumo insumo) {
        for (final var ordem : this.ordens.aguardandoInsumo(insumo.id())) {
            for (final var item : ordem.insumos()) {
                if (item.insumoId().equals(insumo.id()) && item.quantidadePendente() > 0) {
                    final var quantidade = Math.min(insumo.quantidadeDisponivel(), item.quantidadePendente());
                    if (quantidade == 0) {
                        return;
                    }
                    final var reserva = new Quantidade(quantidade);
                    insumo.reservar(reserva);
                    ordem.reservarInsumo(item.id(), reserva);
                }
            }
        }
    }

    private Insumo encontrar(final UUID id) {
        return this.insumos.buscar(id, LockModeType.PESSIMISTIC_WRITE).orElseThrow(InsumoNaoEncontradoException::new);
    }
}
