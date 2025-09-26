package com.redeban.sms.infrastructure.camel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http.HttpMethods;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@ApplicationScoped
public class SmsRoute extends RouteBuilder {
    private static final Logger log = LoggerFactory.getLogger(SmsRoute.class);

    @ConfigProperty(name = "latinia.url")
    String latiniaUrl;

    @ConfigProperty(name = "sms.servicio.host")
    String smsHost;

    @ConfigProperty(name = "sms.servicio.path")
    String smsPath;

    @Override
    public void configure() throws Exception {
        onException(Exception.class)
            .handled(true)
            .process(e -> {
                Exception ex = e.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                String msg = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "unknown";
                String json = "{\"error\":\"" + (msg.replace("\"", "\\\"")) + "\"}";
                e.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
                e.getMessage().setBody(json);
            });

        from("direct:sendSms")
            .routeId("sendSms")
            .process(e -> e.setProperty("startTime", System.currentTimeMillis()))
            .to("velocity:templates/smsRequest.vm")
            .setHeader(Exchange.CONTENT_TYPE, constant("text/xml"))
            .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
            .toD("https://" + smsHost + "/" + smsPath
                + "?bridgeEndpoint=true&sslContextParameters=#sslContextParameters")
            .process(e -> {
                long took = System.currentTimeMillis() - (Long) e.getProperty("startTime");
                log.info("ROUTE sendSms tookMs={} txId={} resp.len={}",
                        took, e.getIn().getHeader("idTransaccion"),
                        (e.getMessage().getBody(String.class) == null ? 0 : e.getMessage().getBody(String.class).length()));
            });

        from("direct:sendSmsLatinia")
            .routeId("sendSmsLatinia")
            .process(e -> e.setProperty("startTime", System.currentTimeMillis()))
            .to("velocity:templates/peticionLatinia.vm")
            .setHeader(Exchange.CONTENT_TYPE, constant("text/xml"))
            .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
            .toD("{{latinia.url}}")
            .process(e -> {
                long took = System.currentTimeMillis() - (Long) e.getProperty("startTime");
                log.info("ROUTE sendSmsLatinia tookMs={} txId={} resp.len={}",
                        took, e.getIn().getHeader("idTransaccion"),
                        (e.getMessage().getBody(String.class) == null ? 0 : e.getMessage().getBody(String.class).length()));
            });
    }

    @ApplicationScoped
    @Named("sslContextParameters")
    SSLContextParameters ssl(
        @ConfigProperty(name = "truststore.path", defaultValue = "") Optional<String> truststorePath,
        @ConfigProperty(name = "truststore.password", defaultValue = "") Optional<String> truststorePassword) {

        if (truststorePath.isEmpty() || truststorePassword.isEmpty()) {
            log.warn("SSLContextParameters no configurado, usando truststore default de la JVM");
            return null;
        }

        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource(truststorePath.get());
        ksp.setPassword(truststorePassword.get());
        ksp.setType("JKS");

        TrustManagersParameters tmp = new TrustManagersParameters();
        tmp.setKeyStore(ksp);

        SSLContextParameters scp = new SSLContextParameters();
        scp.setTrustManagers(tmp);
        return scp;
    }
}
