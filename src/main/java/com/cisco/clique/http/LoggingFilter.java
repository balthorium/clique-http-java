package com.cisco.clique.http;

import org.apache.commons.io.IOUtils;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingFilter implements ClientRequestFilter, ClientResponseFilter {
    private static final Logger LOG = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        LOG.log(Level.INFO, requestContext.getMethod() + " " + requestContext.getUri());
        if (requestContext.hasEntity()) {
            LOG.log(Level.INFO, "request entity:\n" + requestContext.getEntity().toString());
        }
    }

    @Override
    public void filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext) throws IOException {
        LOG.log(Level.INFO, "Response: " + String.valueOf(clientResponseContext.getStatus()));
        if (clientResponseContext.hasEntity()) {
            String entity = IOUtils.toString(clientResponseContext.getEntityStream(), StandardCharsets.UTF_8.name());
            LOG.log(Level.INFO, "response entity:\n" + entity);
        }
    }
}

