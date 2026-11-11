package com.bankbatch.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record RespuestaPagina<T>(
    List<T> contenido,
    int pagina,
    int tamano,
    long totalElementos,
    int totalPaginas) {

    public static <T> RespuestaPagina<T> desde(Page<T> pagina) {
        return new RespuestaPagina<>(
            pagina.getContent(),
            pagina.getNumber(),
            pagina.getSize(),
            pagina.getTotalElements(),
            pagina.getTotalPages());
    }
}
