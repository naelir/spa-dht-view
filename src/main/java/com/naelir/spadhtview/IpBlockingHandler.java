package com.naelir.spadhtview;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

/**
 * Jetty {@link Handler.Wrapper} that rejects requests from IP addresses that
 * are not allowed according to {@link IpRangeFilter#isAllowed(byte[])}.
 *
 * <p>The requester IP is obtained directly from the Jetty
 * {@link Request#getConnectionMetaData()} remote socket address, so it is
 * immune to header spoofing.  Wrap this handler around the
 * {@link org.eclipse.jetty.ee10.servlet.ServletContextHandler} before
 * starting the server.
 */
public class IpBlockingHandler extends Handler.Wrapper {

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        String remoteIp = resolveIp(request);
        if (!isAllowed(remoteIp)) {
            response.setStatus(403);
            byte[] body = "Forbidden".getBytes(StandardCharsets.UTF_8);
            response.write(true, ByteBuffer.wrap(body), callback);
            return true;
        }
        return super.handle(request, response, callback);
    }

    private static String resolveIp(Request request) {
        String xff = request.getHeaders().get("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeaders().get("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri.trim();
        }
        return Request.getRemoteAddr(request);
    }

    private static boolean isAllowed(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        try {
            InetAddress addr = InetAddress.getByName(ip);
            if (addr instanceof Inet6Address) {
                // Range files are IPv4-only; block all native IPv6 connections.
                return false;
            }
            return IpRangeFilter.isAllowed(addr.getAddress());
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
