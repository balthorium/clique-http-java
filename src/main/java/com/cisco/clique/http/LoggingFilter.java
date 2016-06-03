package com.cisco.clique.http;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LoggingFilter implements ClientRequestFilter, ClientResponseFilter {
    private static final Logger LOG = LogManager.getLogger(HttpTransport.class.getName());

    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        LOG.debug("Request: " + clientRequestContext.getMethod() + " " + clientRequestContext.getUri());
    }

    @Override
    public void filter(
            ClientRequestContext clientRequestContext,
            ClientResponseContext clientResponseContext) throws IOException {
        LOG.debug("Response: " + String.valueOf(clientResponseContext.getStatus()));
        if (LOG.isDebugEnabled() && clientResponseContext.hasEntity()) {
            InputStream is = new BufferedInputStream(clientResponseContext.getEntityStream());
            clientResponseContext.setEntityStream(is);
            is.mark(clientResponseContext.getLength());
            String entity = IOUtils.toString(clientResponseContext.getEntityStream(), StandardCharsets.UTF_8.name());
            is.reset();
            LOG.debug("response entity:\n" + entity);
        }
    }
}

