package com.networknt.bot.core;

import com.networknt.client.Http2Client;
import com.networknt.client.simplepool.SimpleConnectionHolder;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class GitUtil {
    private static final Logger logger = LoggerFactory.getLogger(GitUtil.class);

    public static final String HOST = "https://api.github.com";

    static final Http2Client client = Http2Client.getInstance();

    public static boolean branchChanged(String branch, String message) {
        return message.indexOf(branch + "    -> origin/" + branch) > 0 ? true : false;
    }

    public static ClientResponse requestWithBody(String host, String path, HttpString method, Map<String, Object> requestHeader, String requestBody) {
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        SimpleConnectionHolder.ConnectionToken token = null;
        try {
            token = client.borrow(new URI(host), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.EMPTY);
            connection = (ClientConnection) token.getRawConnection();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        try {
            ClientRequest cr = new ClientRequest().setPath(path).setMethod(method);
            if(requestHeader != null) {
                for (Map.Entry<String, Object> entry : requestHeader.entrySet()) {
                    logger.debug("request header key = {} and value = {}", entry.getKey(), entry.getValue());
                    cr.getRequestHeaders().put(new HttpString(entry.getKey()), (String)entry.getValue());
                }
            }
            cr.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(cr, client.createClientCallback(reference, latch, requestBody));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new RuntimeException(e);
        } finally {
            client.restore(token);
        }
        return reference.get();
    }

}
