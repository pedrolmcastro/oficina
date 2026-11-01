package com.oficina.api.apresentacao.auth;

public record RespostaLogin(String tokenAcesso, String tipoToken, long expiraEm) {}
