package br.com.zelador.ingestion.dto;

import com.fasterxml.jackson.databind.ObjectMapper;

public record NotaRequest(
        String numeroNota,
        String chaveAcesso,
        String cnpjEmitente,
        String cnpjDestinatario,
        String valorTotal,
        String emitidaEm
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar payload", e);
        }
    }
}