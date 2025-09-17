package com.redeban.sms.application.rest;

import com.redeban.sms.domain.model.SmsMessage;
import com.redeban.sms.infrastructure.camel.SmsService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/v1/sms")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SmsResource {
    private static final Logger log = LoggerFactory.getLogger(SmsResource.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Inject
    SmsService service;

    @POST
    @Path("/send")
    public Response send(@Valid SmsMessage msg) {
        long t0 = System.nanoTime();
        try {
            log.info("send() IN txId={} input={}", org.slf4j.MDC.get("idTransaccion"), mapper.writeValueAsString(msg));
            var result = service.send(msg);
            long tookMs = (System.nanoTime() - t0) / 1_000_000;
            log.info("send() OK tookMs={} txId={} output={}", tookMs, org.slf4j.MDC.get("idTransaccion"), mapper.writeValueAsString(result));
            return Response.ok(result).build();
        } catch (Exception e) {
            long tookMs = (System.nanoTime() - t0) / 1_000_000;
            log.error("send() FAILED tookMs={} cause='{}' txId={}", tookMs, e.toString(), org.slf4j.MDC.get("idTransaccion"), e);
            return Response.status(500).entity(new ErrorDTO("SMS_SEND_FAILED", e.getMessage())).build();
        }
    }

    public static class ErrorDTO {
        public String code;
        public String message;
        public ErrorDTO(String c, String m) { this.code = c; this.message = m; }
    }
}
