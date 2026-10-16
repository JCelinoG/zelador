package br.com.zelador.domain;

public enum DeliveryStatus {
    PENDENTE,
    EM_PROCESSAMENTO,
    ENTREGUE,
    AGUARDANDO_RETRY,
    DEAD_LETTER
}