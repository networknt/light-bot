package com.networknt.bot.core;

import io.undertow.util.HttpString;
import io.undertow.util.Methods;

public class TestUtil {
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
}
