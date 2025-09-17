package com.redeban.sms.infrastructure.camel;

import com.redeban.sms.domain.model.SmsMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class SmsService {
    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    @Inject
    ProducerTemplate template;

    public Map<String, Object> send(SmsMessage msg) throws Exception {
        Exchange ex = new DefaultExchange(template.getCamelContext());
        ex.getIn().setBody(msg);
        // Headers that the Velocity templates might expect (legacy-like)
        if (msg.from != null) ex.getIn().setHeader("from", msg.from);
        ex.getIn().setHeader("to", msg.to);
        ex.getIn().setHeader("message", msg.message);
        if (msg.channel != null) ex.getIn().setHeader("channel", msg.channel);
        if (msg.campaignId != null) ex.getIn().setHeader("campaignId", msg.campaignId);

        long t0 = System.nanoTime();
        log.debug("Camel send start txId={} to={}...", org.slf4j.MDC.get("idTransaccion"), msg.to);
        Exchange out = template.send("direct:sendSmsLatinia", ex);
        long tookMs = (System.nanoTime() - t0) / 1_000_000;
        log.debug("Camel send end tookMs={} txId={}", tookMs, org.slf4j.MDC.get("idTransaccion"));

        Map<String, Object> result = new HashMap<>();
        result.put("status", out.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, 200));
        result.put("response", out.getMessage().getBody(String.class));
        result.put("tookMs", tookMs);
        result.put("txId", org.slf4j.MDC.get("idTransaccion"));
        return result;
    }
}
