package com.redeban.sms.application.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Provider
@ApplicationScoped
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);
    private static final String START_TIME = "startTimeNano";

    public static final String H_TX = "idTransaccion";
    public static final String H_TS = "timestamp";
    public static final String H_IP = "ipAplicacion";
    public static final String H_APP = "nombreAplicacion";

    @Override
    public void filter(ContainerRequestContext req) throws IOException {
        String tx = firstNonBlank(req.getHeaderString(H_TX), UUID.randomUUID().toString());
        String ts = firstNonBlank(req.getHeaderString(H_TS), Instant.now().toString());
        String ip = firstNonBlank(req.getHeaderString(H_IP), req.getHeaderString("X-Forwarded-For"));
        String app = firstNonBlank(req.getHeaderString(H_APP), "unknown-app");

        MDC.put("idTransaccion", tx);
        MDC.put("timestamp", ts);
        MDC.put("ipAplicacion", ip);
        MDC.put("nombreAplicacion", app);

        req.setProperty(START_TIME, System.nanoTime());
        log.info("REQ IN method={} path={} txId={} fromApp={} fromIp={}",
                req.getMethod(), req.getUriInfo().getPath(), tx, app, ip);
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) throws IOException {
        Long start = (Long) req.getProperty(START_TIME);
        long tookMs = (start != null) ? (System.nanoTime() - start) / 1_000_000 : -1;
        res.getHeaders().add(H_TX, MDC.get("idTransaccion"));
        log.info("RES OUT status={} tookMs={} txId={}", res.getStatus(), tookMs, MDC.get("idTransaccion"));
        MDC.clear();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        return (b != null && !b.isBlank()) ? b : null;
    }
}
