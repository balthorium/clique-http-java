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
import static org.testng.AssertJUnit.assertNull;

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
        assertNotNull(key2);
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

        _transport.putIdChain(chain1);

        _transport.clear();

        AbstractChain chain2 = _transport.getIdChain(new IdBlockValidator(_transport, _trustRoots), _aliceUri);
        assertNotNull(chain2);
        assertEquals(chain2, chain1);
    }

    @Test
    public void putMultiBlockIdChainGetMultiBLockIdChainTest() throws Exception {

        ECKey key1 = generateKeyPair();
        _transport.putKey(key1);

        IdChain chain1 = new IdChain(new IdBlockValidator(_transport, _trustRoots));
        chain1.newBlockBuilder()
                .setIssuer(_aliceUri)
                .setIssuerKey(key1)
                .setSubject(_aliceUri)
                .setSubjectPubKey(key1.toPublicJWK())
                .build();

        ECKey key2 = generateKeyPair();
        _transport.putKey(key2);

        chain1.newBlockBuilder()
                .setIssuer(_aliceUri)
                .setIssuerKey(key1)
                .setSubject(_aliceUri)
                .setSubjectPubKey(key2.toPublicJWK())
                .build();

        _transport.putIdChain(chain1);

        _transport.clear();

        AbstractChain chain2 = _transport.getIdChain(new IdBlockValidator(_transport, _trustRoots), _aliceUri);
        assertNotNull(chain2);
        assertEquals(chain2, chain1);
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

        _transport.putIdChain(idChain);

        AuthChain chain1 = new AuthChain(new AuthBlockValidator(_transport, _trustRoots));
        chain1.newBlockBuilder()
                .setIssuer(_aliceUri)
                .setIssuerKey(key)
                .setSubject(_resourceUri)
                .addGrant(new AuthBlock.Grant(AuthBlock.Grant.Type.VIRAL_GRANT, _bobUri, _privilege))
                .build();
        _transport.putAuthChain(chain1);

        _transport.clear();

        AuthChain chain2 = (AuthChain) _transport.getAuthChain(new AuthBlockValidator(_transport, _trustRoots), chain1.getSubject());
        assertNotNull(chain2);
        assertEquals(chain2, chain1);
    }

    @Test
    public void putMultiBlockAuthChainGetMultiBlockAuthChainTest() throws Exception {
        ECKey keyAlice = generateKeyPair();
        _transport.putKey(keyAlice);

        IdChain idChainAlice = new IdChain(new IdBlockValidator(_transport, _trustRoots));
        idChainAlice.newBlockBuilder()
                .setIssuer(_aliceUri)
                .setIssuerKey(keyAlice)
                .setSubject(_aliceUri)
                .setSubjectPubKey(keyAlice.toPublicJWK())
                .build();

        _transport.putIdChain(idChainAlice);

        ECKey keyBob = generateKeyPair();
        _transport.putKey(keyBob);

        IdChain idChainBob = new IdChain(new IdBlockValidator(_transport, _trustRoots));
        idChainBob.newBlockBuilder()
                .setIssuer(_bobUri)
                .setIssuerKey(keyBob)
                .setSubject(_bobUri)
                .setSubjectPubKey(keyBob.toPublicJWK())
                .build();

        _transport.putIdChain(idChainBob);

        AuthChain authChain1 = new AuthChain(new AuthBlockValidator(_transport, _trustRoots));
        authChain1.newBlockBuilder()
                .setIssuer(_aliceUri)
                .setIssuerKey(keyAlice)
                .setSubject(_resourceUri)
                .addGrant(new AuthBlock.Grant(AuthBlock.Grant.Type.VIRAL_GRANT, _aliceUri, _privilege))
                .addGrant(new AuthBlock.Grant(AuthBlock.Grant.Type.GRANT, _bobUri, _privilege))
                .build();

        authChain1.newBlockBuilder()
                .setIssuer(_aliceUri)
                .setIssuerKey(keyAlice)
                .setSubject(_resourceUri)
                .addGrant(new AuthBlock.Grant(AuthBlock.Grant.Type.REVOKE, _bobUri, _privilege))
                .build();

        _transport.putAuthChain(authChain1);

        _transport.clear();

        AuthChain authChain2 = (AuthChain) _transport.getAuthChain(new AuthBlockValidator(_transport, _trustRoots), authChain1.getSubject());
        assertNotNull(authChain2);
        assertEquals(authChain2, authChain1);
    }

    @Test
    public void getNonExistingKeyTest() throws Exception {
        ECKey key1 = generateKeyPair();
        ECKey key2 = _transport.getKey(key1.computeThumbprint().toString());
        assertNull(key2);
    }

    @Test
    public void getNonExistingIdChainTest() throws Exception {
        AbstractChain chain2 = _transport.getIdChain(new IdBlockValidator(_transport, _trustRoots), _aliceUri);
        assertNull(chain2);
    }

    @Test
    public void getNonExistingAuthChainTest() throws Exception {
        AbstractChain chain2 = _transport.getAuthChain(new AuthBlockValidator(_transport, _trustRoots), _resourceUri);
        assertNull(chain2);
    }
}
