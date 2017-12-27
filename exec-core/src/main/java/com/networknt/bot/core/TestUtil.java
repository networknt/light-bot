package com.networknt.bot.core;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class TestUtil {
    static final Logger logger = LoggerFactory.getLogger(TestUtil.class);

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
        }
        return httpString;
    }

    public static boolean matchHeader(Map<String, Object> header, HeaderMap headerMap) {
        boolean matched = true;
        for (Map.Entry<String, Object> entry : header.entrySet()) {
            logger.debug("header key = {} and value = {}", entry.getKey(), entry.getValue());
            if(!entry.getValue().equals(headerMap.get(entry.getKey(), 0))) {
                matched = false;
                logger.error("header {} value {} doesn't match the response header value {}", entry.getKey(), entry.getValue(), headerMap.get(entry.getKey(), 0));
                break;
            }
        }
        return matched;
    }

    public static boolean matchBody(Map<String, Object> body, String responseBody) {
        boolean matched = true;
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            logger.debug("body key = {} and value = {}", entry.getKey(), entry.getValue());

            if(!entry.getValue().equals(JsonPath.read(responseBody, entry.getKey()))) {
                matched = false;
                logger.error("body key {} value {} doesn't match the response body value {}", entry.getKey(), entry.getValue(), JsonPath.read(responseBody, entry.getKey()));
                break;
            }
        }
        return matched;
    }
}
