package com.oficina.api.dominio.veiculo;

import com.oficina.api.dominio.DadoInvalidoException;

public record Placa(String valor) {
    public Placa {
        if (!valor.matches("[A-Z]{3}\\d{4}|[A-Z]{3}\\d[A-Z]\\d{2}")) {
            throw new DadoInvalidoException("Placa deve seguir o formato ABC1234 ou ABC1D23, sem hífen");
        }
    }

    @Override
    public String toString() {
        return "Placa[valor=*]";
    }
}
