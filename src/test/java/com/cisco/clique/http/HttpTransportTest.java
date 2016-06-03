package com.cisco.clique.http;

import com.cisco.clique.sdk.chains.AbstractChain;
import com.cisco.clique.sdk.chains.AuthBlock;
import com.cisco.clique.sdk.chains.AuthChain;
import com.cisco.clique.sdk.chains.IdChain;
import com.cisco.clique.sdk.validation.AuthBlockValidator;
import com.cisco.clique.sdk.validation.IdBlockValidator;
import com.nimbusds.jose.jwk.ECKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class HttpTransportTest {

    private HttpTransport _transport;
    private Set<String> _trustRoots;
    private URL _serviceUrl;
    private URI _aliceUri;
    private URI _bobUri;
    private URI _resourceUri;
    private String _privilege;

    @BeforeTest
    public void suiteSetUp() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        _serviceUrl = new URL("https://cliquey.io");
        _privilege = "read";
    }

    @BeforeMethod
    public void testSetUp() {
        _transport = new HttpTransport(_serviceUrl);
        _trustRoots = new HashSet<>();
        _aliceUri = URI.create("uri:clique:alice:" + UUID.randomUUID().toString());
        _bobUri = URI.create("uri:clique:bob:" + UUID.randomUUID().toString());
        _resourceUri = URI.create("http://example.com/some/distributed/resource" + UUID.randomUUID().toString());
    }

    ECKey generateKeyPair() throws Exception {
        ECKey.Curve crv = ECKey.Curve.P_256;
        KeyPairGenerator gen = KeyPairGenerator.getInstance("ECDSA");
        gen.initialize(crv.toECParameterSpec());
        KeyPair pair = gen.generateKeyPair();
        return new ECKey.Builder(crv, (ECPublicKey) pair.getPublic())
                .privateKey((ECPrivateKey) pair.getPrivate())
                .build();
    }

    @Test
    public void putKeyGetKeyTest() throws Exception {
        ECKey key1 = generateKeyPair();
        _transport.putKey(key1.toPublicJWK());
        _transport.clear();
        ECKey key2 = _transport.getKey(key1.computeThumbprint().toString());
        assertEquals(key2.computeThumbprint(), key1.computeThumbprint());
    }

    @Test
    public void putIdChainGetIdChainTest() throws Exception {

        ECKey key = generateKeyPair();
        _transport.putKey(key);

        IdChain chain1 = new IdChain(new IdBlockValidator(_transport, _trustRoots));
        chain1.newBlockBuilder()
                .setIssuer(_aliceUri)
                .setIssuerKey(key)
                .setSubject(_aliceUri)
                .setSubjectPubKey(key.toPublicJWK())
                .build();

        _transport.putChain(chain1);

        _transport.clear();

        AbstractChain chain2 = _transport.getIdChain(new IdBlockValidator(_transport, _trustRoots), _aliceUri);
        assertNotNull(chain2);
    }


    @Test
    public void putAuthChainGetAuthChainTest() throws Exception {
        ECKey key = generateKeyPair();
        _transport.putKey(key);

        IdChain idChain = new IdChain(new IdBlockValidator(_transport, _trustRoots));
        idChain.newBlockBuilder()
                .setIssuer(_aliceUri)
                .setIssuerKey(key)
                .setSubject(_aliceUri)
                .setSubjectPubKey(key.toPublicJWK())
                .build();

        _transport.putChain(idChain);

        AuthChain chain1 = new AuthChain(new AuthBlockValidator(_transport, _trustRoots));
        chain1.newBlockBuilder()
                .setIssuer(_aliceUri)
                .setIssuerKey(key)
                .setSubject(_resourceUri)
                .addGrant(new AuthBlock.Grant(AuthBlock.Grant.Type.VIRAL_GRANT, _bobUri, _privilege))
                .build();
        _transport.putChain(chain1);

        _transport.clear();

        AuthChain chain2 = (AuthChain) _transport.getAuthChain(new AuthBlockValidator(_transport, _trustRoots), chain1.getSubject());
        assertEquals(chain2, chain1);
    }
}
