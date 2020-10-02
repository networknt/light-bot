package com.networknt.bot.core;

import java.io.InputStream;
import java.io.OutputStream;

public interface InOutErrStreamProvider {
    InputStream getInputStream();
    OutputStream getOutputStream();
    OutputStream getErrorStream();
}
