package com.cisco.clique.http;

import com.cisco.clique.http.dto.PublicKey;
import com.cisco.clique.sdk.MemoryTransport;
import com.cisco.clique.sdk.Transport;
import com.cisco.clique.sdk.chains.AbstractChain;
import com.nimbusds.jose.jwk.ECKey;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.filter.LoggingFilter;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.net.URI;

public class HttpTransport implements Transport {

    public static final String GET_KEY_REQUEST_URL_TEMPLATE =
            "https://cliquey.io/api/v1/keys/{pkt}";

    public static final String PUT_KEY_REQUEST_URL_TEMPLATE =
            "https://cliquey.io/api/v1/keys";

    private MemoryTransport _cache;
    private Client _client;

    public HttpTransport() {
        _cache = new MemoryTransport();

        boolean useProxy = false;
        if (useProxy) {
            _client = ClientBuilder.newClient(new ClientConfig()
                    .connectorProvider(new ApacheConnectorProvider())
                    .property(ClientProperties.PROXY_URI, "http://localhost:8080")
                    .register(LoggingFilter.class));
        } else {
            _client = ClientBuilder.newClient(new ClientConfig()
                    .register(LoggingFilter.class));
        }
    }

    @Override
    public void putKey(ECKey key) throws Exception {
        _cache.putKey(key);
        _client.target(PUT_KEY_REQUEST_URL_TEMPLATE)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(new PublicKey(key), MediaType.APPLICATION_JSON_TYPE));
    }

    @Override
    public ECKey getKey(String pkt) throws Exception {
        ECKey key = _cache.getKey(pkt);
        if (null == key) {
            PublicKey keyDto = _client.target(GET_KEY_REQUEST_URL_TEMPLATE)
                    .resolveTemplate("pkt", pkt)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(PublicKey.class);
            key = keyDto.getKey();
            _cache.putKey(key);
        }
        return key;
    }

    @Override
    public void putChain(AbstractChain abstractChain) throws Exception {
        _cache.putChain(abstractChain);
    }

    @Override
    public AbstractChain getChain(URI uri) throws Exception {
        return _cache.getChain(uri);
    }

    @Override
    public void clear() {
        _cache.clear();
    }
}
