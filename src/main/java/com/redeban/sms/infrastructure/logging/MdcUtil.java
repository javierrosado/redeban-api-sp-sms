package com.redeban.sms.infrastructure.logging;

import org.slf4j.MDC;

public final class MdcUtil {
    private MdcUtil() {}

    public static String txId() { return MDC.get("idTransaccion"); }
    public static String timestamp() { return MDC.get("timestamp"); }
    public static String ipAplicacion() { return MDC.get("ipAplicacion"); }
    public static String nombreAplicacion() { return MDC.get("nombreAplicacion"); }
}
