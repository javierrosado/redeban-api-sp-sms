package com.redeban.sms.infrastructure.camel;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http.HttpMethods;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SmsRoute extends RouteBuilder {

    @ConfigProperty(name = "latinia.url")
    String latiniaUrl;

    @Override
    public void configure() throws Exception {

        // Handler de errores genérico: responde JSON y HTTP 500
        onException(Exception.class)
            .handled(true)
            .log("ERROR in route: ${exception.class} - ${exception.message}")
            .process(e -> {
                Exception ex = e.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                String msg = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "unknown";
                String json = "{\"error\":\"" + msg.replace("\"","\\\"") + "\"}";
                e.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
                e.getMessage().setBody(json);
            });

        // Ruta equivalente al legado direct-vm:sendSms (WMB + Velocity)
        from("direct:sendSms")
            .routeId("sendSms")
            .to("velocity:templates/smsRequest.vm")
            .setHeader(Exchange.CONTENT_TYPE, constant("text/xml"))
            .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
            .toD("https://{{sms.servicio.host}}/{{sms.servicio.path}}?bridgeEndpoint=true")
            .log("Respuesta backend WMB: ${body}");

        // Ruta equivalente al legado direct-vm:sendSmsLatinia (Latinia + Velocity)
        from("direct:sendSmsLatinia")
            .routeId("sendSmsLatinia")
            .to("velocity:templates/peticionLatinia.vm")
            .setHeader(Exchange.CONTENT_TYPE, constant("text/xml"))
            .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
            .toD("{{latinia.url}}")
            .log("Respuesta Latinia: ${body}");
    }
}
