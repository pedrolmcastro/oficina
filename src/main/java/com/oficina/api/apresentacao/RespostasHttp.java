package com.oficina.api.apresentacao;

import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class RespostasHttp {
    private RespostasHttp() {}

    public static ResponseEntity.BodyBuilder status(final HttpStatus status) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore());
    }

    public static <T> ResponseEntity<T> ok(final T corpo) {
        return status(HttpStatus.OK).body(corpo);
    }

    public static <T> ResponseEntity<T> criada(final URI localizacao, final T corpo) {
        return status(HttpStatus.CREATED).location(localizacao).body(corpo);
    }

    public static ResponseEntity<Void> semConteudo() {
        return status(HttpStatus.NO_CONTENT).build();
    }
}
