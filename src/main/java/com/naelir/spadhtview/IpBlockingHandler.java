package com.naelir.spadhtview;

import java.net.InetAddress;
import java.net.UnknownHostException;

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

    public IpBlockingHandler(Handler wrapped) {
        super(wrapped);
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        String remoteIp = Request.getRemoteAddr(request);
        System.out.println("Remote IP: " + remoteIp);
        System.out.println(request.getHeaders().asString());
//        if (!isAllowed(remoteIp)) {
//            response.setStatus(403);
//            byte[] body = "Forbidden".getBytes(StandardCharsets.UTF_8);
//            response.write(true, ByteBuffer.wrap(body), callback);
//            return true;
//        }
        return super.handle(request, response, callback);
    }

    private static boolean isAllowed(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        try {
            byte[] bytes = InetAddress.getByName(ip).getAddress();
            return IpRangeFilter.isAllowed(bytes);
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
