package com.oficina.api.aplicacao;

import com.oficina.api.dominio.DadoInvalidoException;
import com.oficina.api.dominio.Entidade;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record Pagina<T extends Entidade>(
    List<T> itens,
    @Nullable
    UUID nextCursor
) {
    public static record Limite(int valor) {
        public static final int MAXIMO = 25;

        public Limite {
            if (valor < 1 || valor > MAXIMO) {
                throw new DadoInvalidoException("first deve estar entre 1 e " + MAXIMO);
            }
        }

        public int quantidadeParaConsulta() {
            return this.valor + 1;
        }
    }

    public Pagina {
        itens = List.copyOf(itens);
    }

    public static <T extends Entidade> Pagina<T> de(final List<T> encontrados, final Limite limite) {
        final var temProximaPagina = encontrados.size() > limite.valor();
        final var itens = temProximaPagina
            ? encontrados.subList(0, limite.valor())
            : encontrados;
        return new Pagina<>(
            itens,
            temProximaPagina
                ? itens.getLast().id()
                : null
        );
    }
}
