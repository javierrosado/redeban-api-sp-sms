package com.redeban.sms.domain.model;

import jakarta.validation.constraints.NotBlank;

public class SmsMessage {
    @NotBlank
    public String to;

    @NotBlank
    public String message;

    public String from;
    public String channel;
    public String campaignId;
}
