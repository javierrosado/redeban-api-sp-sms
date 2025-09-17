package com.redeban.sms.domain.model;

import jakarta.validation.constraints.NotBlank;

public class SmsMessage {
    @NotBlank
    public String to;         // MSISDN destino
    @NotBlank
    public String message;    // Contenido del SMS
    public String from;       // Remitente (opcional)
    public String channel;    // Canal (opcional)
    public String campaignId; // Id campaña (opcional)
}
