package com.cisco.clique.http;

import com.cisco.clique.http.dto.DtoPublicKey;
import com.cisco.clique.sdk.MemoryTransport;
import com.cisco.clique.sdk.Transport;
import com.cisco.clique.sdk.chains.*;
import com.cisco.clique.sdk.validation.AbstractValidator;
import com.nimbusds.jose.jwk.ECKey;
import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
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

    private URL _serviceUrl;
    private MemoryTransport _cache;
    private Client _client;

    public HttpTransport(URL serviceUrl) {
        _serviceUrl = serviceUrl;
        _cache = new MemoryTransport();
        String proxyUrl = System.getProperty("clique.proxy.url");
        if (null == proxyUrl) {
            _client = ClientBuilder.newClient(new ClientConfig())
                    .register(AndroidFriendlyFeature.class);
        } else {
            _client = ClientBuilder.newClient(new ClientConfig()
                    .connectorProvider(new ApacheConnectorProvider())
                    .property(ClientProperties.PROXY_URI, proxyUrl)
                    .register(AndroidFriendlyFeature.class));
        }
    }

    @Override
    public void putKey(ECKey key) throws Exception {
        if (null == _cache.getKey(key.computeThumbprint().toString())) {
            _cache.putKey(key);
            _client.target(PUT_KEY_REQUEST_URL_TEMPLATE)
                    .resolveTemplateFromEncoded("serviceUrl", _serviceUrl)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(new DtoPublicKey(key), MediaType.APPLICATION_JSON_TYPE));
        }
    }

    @Override
    public ECKey getKey(String pkt) throws Exception {
        ECKey key = _cache.getKey(pkt);
        if (null == key) {
            try {
                DtoPublicKey keyDto = _client.target(GET_KEY_REQUEST_URL_TEMPLATE)
                        .resolveTemplateFromEncoded("serviceUrl", _serviceUrl)
                        .resolveTemplate("pkt", pkt)
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get(DtoPublicKey.class);
                key = keyDto.getKey();
                _cache.putKey(key);
            } catch (NotFoundException nfe) {
                key = null;
            }
        }
        return key;
    }

    private void putChain(AbstractChain<? extends AbstractBlock> abstractChain) throws Exception {
        for (AbstractBlock block : abstractChain.getBlocks()) {
            _client.target(PUT_CHAIN_REQUEST_URL_TEMPLATE)
                    .resolveTemplateFromEncoded("serviceUrl", _serviceUrl)
                    .resolveTemplate("uri", abstractChain.getSubject().toString())
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(block.serialize(), "application/jose"));
        }
    }

    public String getChain(URI uri) throws Exception {
        try {
            return _client.target(GET_CHAIN_REQUEST_URL_TEMPLATE)
                    .resolveTemplateFromEncoded("serviceUrl", _serviceUrl)
                    .resolveTemplate("uri", uri)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(String.class);
        } catch (NotFoundException nfe) {
            return null;
        }
    }

    @Override
    public void putIdChain(AbstractChain<IdBlock> abstractChain) throws Exception {
        _cache.putIdChain(abstractChain);
        putChain(abstractChain);
    }

    @Override
    public AbstractChain<IdBlock> getIdChain(AbstractValidator<IdBlock> validator, URI uri) throws Exception {
        AbstractChain<IdBlock> chain = _cache.getIdChain(validator, uri);
        if (null == chain) {
            String serialization = getChain(uri);
            if (null != serialization) {
                chain = new IdChain(validator, serialization);
            }
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
        AbstractChain<AuthBlock> chain = _cache.getAuthChain(validator, uri);
        if (null == chain) {
            String serialization = getChain(uri);
            if (null != serialization) {
                chain = new AuthChain(validator, serialization);
            }
        }
        return chain;
    }

    @Override
    public void clear() {
        _cache.clear();
    }

    private static class AndroidFriendlyFeature implements Feature {
        @Override
        public boolean configure(FeatureContext context) {
            context.register(new AbstractBinder() {
                @Override
                protected void configure() {
                    addUnbindFilter(new Filter() {
                        @Override
                        public boolean matches(Descriptor d) {
                            String implClass = d.getImplementation();
                            return implClass.startsWith(
                                    "org.glassfish.jersey.message.internal.DataSource")
                                    || implClass.startsWith(
                                    "org.glassfish.jersey.message.internal.RenderedImage");
                        }
                    });
                }
            });
            return true;
        }
    }
}
