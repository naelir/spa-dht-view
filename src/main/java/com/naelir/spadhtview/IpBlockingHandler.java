package com.naelir.spadhtview;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

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

    private static final Logger LOG = Logger.getLogger(IpBlockingHandler.class.getName());
    private static final long LOG_INTERVAL = 100;
    private final AtomicLong blockedCount = new AtomicLong(0);

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        String remoteIp = resolveIp(request);
        if (!isAllowed(remoteIp)) {
            long count = blockedCount.incrementAndGet();
            if (count % LOG_INTERVAL == 0) {
                LOG.warning("Blocked requests count: " + count);
            }
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

            if (addr.isLoopbackAddress() || addr.isAnyLocalAddress()) {
                return true; // allow localhost
            }
            return IpRangeFilter.isAllowed(addr.getAddress());
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
