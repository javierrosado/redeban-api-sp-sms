package com.redeban.sms.domain.model;

public class SmsSendResult {
    public int status;
    public String response;
    public long tookMs;
    public String txId;

    public SmsSendResult() {}

    public SmsSendResult(int status, String response, long tookMs, String txId) {
        this.status = status;
        this.response = response;
        this.tookMs = tookMs;
        this.txId = txId;
    }
}
