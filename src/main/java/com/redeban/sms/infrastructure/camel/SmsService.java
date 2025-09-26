package com.redeban.sms.infrastructure.camel;

import com.redeban.sms.domain.model.SmsMessage;
import com.redeban.sms.domain.model.SmsSendResult;
import com.redeban.sms.infrastructure.logging.JsonLog;
import com.redeban.sms.infrastructure.logging.MdcUtil;
import com.redeban.sms.infrastructure.logging.Stopwatch;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ApplicationScoped
public class SmsService {
    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    @Inject
    ProducerTemplate template;

    public SmsSendResult send(SmsMessage msg) throws Exception {
        final var sw = Stopwatch.start();
        final String tx = MdcUtil.txId();

        Exchange ex = new DefaultExchange(template.getCamelContext());
        ex.getIn().setBody(msg);
        ex.getIn().setHeader("to", msg.to);
        ex.getIn().setHeader("message", msg.message);
        if (msg.from != null) ex.getIn().setHeader("from", msg.from);
        if (msg.channel != null) ex.getIn().setHeader("channel", msg.channel);
        if (msg.campaignId != null) ex.getIn().setHeader("campaignId", msg.campaignId);

        ex.getIn().setHeader("idTransaccion", MdcUtil.txId());
        ex.getIn().setHeader("timestamp", MdcUtil.timestamp());
        ex.getIn().setHeader("ipAplicacion", MdcUtil.ipAplicacion());
        ex.getIn().setHeader("nombreAplicacion", MdcUtil.nombreAplicacion());

        log.debug("SERVICE IN send txId={} input={}", tx, JsonLog.toJson(Map.of(
                "to", msg.to, "message.len", (msg.message == null ? 0 : msg.message.length())
        )));

        Exchange out = template.send("direct:sendSmsLatinia", ex);

        int status = out.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, 200, int.class);
        String body = out.getMessage().getBody(String.class);
        long took = sw.elapsedMs();

        log.debug("SERVICE OUT send txId={} tookMs={} output={}", tx, took, JsonLog.toJson(Map.of(
                "status", status, "response.len", (body == null ? 0 : body.length())
        )));

        return new SmsSendResult(status, body, took, tx);
    }
}
