package com.cisco.clique.http;

import com.cisco.clique.http.dto.DtoPublicKey;
import com.cisco.clique.sdk.MemoryTransport;
import com.cisco.clique.sdk.Transport;
import com.cisco.clique.sdk.chains.*;
import com.cisco.clique.sdk.validation.AbstractValidator;
import com.nimbusds.jose.jwk.ECKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URL;

public class HttpTransport implements Transport {
    public static final String PUT_KEY_REQUEST_URL_TEMPLATE =
            "{serviceUrl}/api/v1/keys";
    public static final String GET_KEY_REQUEST_URL_TEMPLATE =
            "{serviceUrl}/api/v1/keys/{pkt}";
    public static final String PUT_CHAIN_REQUEST_URL_TEMPLATE =
            "{serviceUrl}/api/v1/chains/{uri}";
    public static final String GET_CHAIN_REQUEST_URL_TEMPLATE =
            "{serviceUrl}/api/v1/chains/{uri}";
    private static final Logger LOG = LogManager.getLogger(HttpTransport.class.getName());
    private URL _serviceUrl;
    private MemoryTransport _cache;
    private Client _client;

    public HttpTransport(URL serviceUrl) {
        _serviceUrl = serviceUrl;
        _cache = new MemoryTransport();
        String proxyUrl = System.getProperty("clique.proxy.url");
        if (null == proxyUrl) {
            _client = ClientBuilder.newClient(new ClientConfig()
                    .register(new LoggingFilter()));
        } else {
            _client = ClientBuilder.newClient(new ClientConfig()
                    .connectorProvider(new ApacheConnectorProvider())
                    .property(ClientProperties.PROXY_URI, proxyUrl)
                    .register(new LoggingFilter()));
        }
    }

    @Override
    public void putKey(ECKey key) throws Exception {
        if (null == _cache.getKey(key.computeThumbprint().toString())) {
            LOG.debug("Put key: " + key.computeThumbprint());
            _cache.putKey(key);
            _client.target(PUT_KEY_REQUEST_URL_TEMPLATE)
                    .resolveTemplateFromEncoded("serviceUrl", _serviceUrl)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(new DtoPublicKey(key), MediaType.APPLICATION_JSON_TYPE));
        }
    }

    @Override
    public ECKey getKey(String pkt) throws Exception {
        LOG.debug("Get key: " + pkt);
        ECKey key = _cache.getKey(pkt);
        if (null == key) {
            LOG.debug("Cache miss on key: " + pkt);
            DtoPublicKey keyDto = _client.target(GET_KEY_REQUEST_URL_TEMPLATE)
                    .resolveTemplateFromEncoded("serviceUrl", _serviceUrl)
                    .resolveTemplate("pkt", pkt)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(DtoPublicKey.class);
            key = keyDto.getKey();
            _cache.putKey(key);
        }
        return key;
    }

    private void putChain(AbstractChain<? extends AbstractBlock> abstractChain) throws Exception {
        LOG.debug("Put chain: " + abstractChain.getSubject());
        for (AbstractBlock block : abstractChain.getBlocks()) {
            _client.target(PUT_CHAIN_REQUEST_URL_TEMPLATE)
                    .resolveTemplateFromEncoded("serviceUrl", _serviceUrl)
                    .resolveTemplate("uri", abstractChain.getSubject().toString())
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(block.serialize(), "application/jose"));
        }
    }

    public String getChain(URI uri) throws Exception {
        return _client.target(GET_CHAIN_REQUEST_URL_TEMPLATE)
                .resolveTemplateFromEncoded("serviceUrl", _serviceUrl)
                .resolveTemplate("uri", uri)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(String.class);
    }

    @Override
    public void putIdChain(AbstractChain<IdBlock> abstractChain) throws Exception {
        _cache.putIdChain(abstractChain);
        putChain(abstractChain);
    }

    @Override
    public AbstractChain<IdBlock> getIdChain(AbstractValidator<IdBlock> validator, URI uri) throws Exception {
        LOG.debug("Get ID chain: " + uri.toString());
        AbstractChain<IdBlock> chain = _cache.getIdChain(validator, uri);
        if (null == chain) {
            LOG.debug("Cache miss on ID chain: " + uri.toString());
            chain = new IdChain(validator, getChain(uri));
        }
        return chain;
    }

    @Override
    public void putAuthChain(AbstractChain<AuthBlock> abstractChain) throws Exception {
        _cache.putAuthChain(abstractChain);
        putChain(abstractChain);
    }

    @Override
    public AbstractChain<AuthBlock> getAuthChain(AbstractValidator<AuthBlock> validator, URI uri) throws Exception {
        LOG.debug("Get auth chain: " + uri.toString());
        AbstractChain<AuthBlock> chain = _cache.getAuthChain(validator, uri);
        if (null == chain) {
            LOG.debug("Cache miss on auth chain: " + uri.toString());
            chain = new AuthChain(validator, getChain(uri));
        }
        return chain;
    }

    @Override
    public void clear() {
        _cache.clear();
    }
}
