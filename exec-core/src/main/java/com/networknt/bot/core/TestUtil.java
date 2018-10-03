package com.networknt.bot.core;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import com.networknt.client.Http2Client;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.net.URI;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class TestUtil {
    static final Logger logger = LoggerFactory.getLogger(TestUtil.class);
    static final Http2Client client = Http2Client.getInstance();

    static {
        Configuration.setDefaults(new Configuration.Defaults() {

            private final JsonProvider jsonProvider = new JacksonJsonProvider();
            private final MappingProvider mappingProvider = new JacksonMappingProvider();

            @Override
            public JsonProvider jsonProvider() {
                return jsonProvider;
            }

            @Override
            public MappingProvider mappingProvider() {
                return mappingProvider;
            }

            @Override
            public Set<Option> options() {
                return EnumSet.noneOf(Option.class);
            }
        });
    }
    public static HttpString toHttpString(String method) {
        HttpString httpString = null;
        switch (method) {
            case "get":
                httpString = Methods.GET;
                break;
            case "post":
                httpString = Methods.POST;
                break;
            case "delete":
                httpString = Methods.DELETE;
                break;
            case "patch":
                httpString = Methods.PATCH;
                break;
            case "put":
                httpString = Methods.PUT;
                break;
            case "options":
                httpString = Methods.OPTIONS;
                break;
        }
        return httpString;
    }

    public static boolean matchHeader(Map<String, Object> header, HeaderMap headerMap) {
        boolean matched = true;
        if(header != null) {
            for (Map.Entry<String, Object> entry : header.entrySet()) {
                logger.debug("header key = {} and value = {}", entry.getKey(), entry.getValue());
                if(!entry.getValue().equals(headerMap.get(entry.getKey(), 0))) {
                    matched = false;
                    logger.error("header {} value {} doesn't match the response header value {}", entry.getKey(), entry.getValue(), headerMap.get(entry.getKey(), 0));
                    break;
                }
            }
        }
        return matched;
    }

    public static boolean matchBody(Map<String, Object> body, String responseBody) {
        boolean matched = true;
        if(body != null) {
            for (Map.Entry<String, Object> entry : body.entrySet()) {
                logger.debug("body key = {} and value = {}", entry.getKey(), entry.getValue());

                if(!entry.getValue().equals(JsonPath.read(responseBody, entry.getKey()))) {
                    matched = false;
                    logger.error("body key {} value {} doesn't match the response body value {}", entry.getKey(), entry.getValue(), JsonPath.read(responseBody, entry.getKey()));
                    break;
                }
            }
        }
        return matched;
    }

    public static ClientResponse request(String host, String path, HttpString method, Map<String, Object> requestHeader) {
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(host), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        } catch (Exception e) {
            logger.error("request Exception: " + " host = " + host + " path = " + path + " method = " + method + " header = " + requestHeader, e);
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
            cr.getRequestHeaders().put(Headers.HOST, "localhost");
            connection.sendRequest(cr, client.createClientCallback(reference, latch));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new RuntimeException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        return reference.get();
    }

    public static ClientResponse requestWithBody(String host, String path, HttpString method, Map<String, Object> requestHeader, String requestBody) {
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(host), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
        } catch (Exception e) {
            logger.error("requestWithBody Exception: " + " host = " + host + " path = " + path + " method = " + method + " header = " + requestHeader + " requestBody = " + requestBody, e);
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
            cr.getRequestHeaders().put(Headers.HOST, "localhost");
            cr.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            connection.sendRequest(cr, client.createClientCallback(reference, latch, requestBody));
            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new RuntimeException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        return reference.get();
    }

}
