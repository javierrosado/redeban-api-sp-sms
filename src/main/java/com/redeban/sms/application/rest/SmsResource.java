package com.redeban.sms.application.rest;

import com.redeban.sms.domain.model.SmsMessage;
import com.redeban.sms.domain.model.SmsSendResult;
import com.redeban.sms.infrastructure.camel.SmsService;
import com.redeban.sms.infrastructure.logging.JsonLog;
import com.redeban.sms.infrastructure.logging.MdcUtil;
import com.redeban.sms.infrastructure.logging.Stopwatch;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Path("/v1/sms")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "SMS", description = "Envío de mensajes SMS")
public class SmsResource {
    private static final Logger log = LoggerFactory.getLogger(SmsResource.class);

    @Inject
    SmsService service;

    @POST
    @Path("/send")
    @Operation(summary = "Envia un SMS via backend (Latinia/WMB)")
    @APIResponse(responseCode = "200", description = "SMS enviado",
        content = @Content(schema = @Schema(implementation = SmsSendResult.class)))
    public Response send(@Valid SmsMessage msg) {
        final var sw = Stopwatch.start();
        final String tx = MdcUtil.txId();

        log.info("METHOD IN send txId={} input={}", tx, JsonLog.toJson(Map.of(
                "to", msg.to,
                "message", msg.message,
                "from", msg.from,
                "channel", msg.channel,
                "campaignId", msg.campaignId
        )));

        try {
            SmsSendResult out = service.send(msg);
            long took = sw.elapsedMs();
            log.info("METHOD OUT send txId={} tookMs={} output={}", tx, took, JsonLog.toJson(Map.of(
                    "status", out.status,
                    "tookMs", out.tookMs,
                    "response.len", (out.response == null ? 0 : out.response.length())
            )));
            return Response.ok(out).build();
        } catch (Exception e) {
            long took = sw.elapsedMs();
            log.error("METHOD ERR send txId={} tookMs={} cause='{}'", tx, took, e.toString(), e);
            return Response.status(500).entity(Map.of(
                    "code", "SMS_SEND_FAILED",
                    "message", e.getMessage(),
                    "txId", tx
            )).build();
        }
    }
}
