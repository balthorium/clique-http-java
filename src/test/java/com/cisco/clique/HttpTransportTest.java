package com.cisco.clique.sdk;

import com.cisco.clique.http.HttpTransport;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URL;
import java.security.Security;

public class HttpTransportTest {

    private URL _serviceUrl;
    private URL _proxyUrl;

    @BeforeTest
    public void suiteSetUp() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        _serviceUrl = new URL("https://cliquey.io");
        _proxyUrl = new URL("http://localhost:8080");
    }

    @BeforeMethod
    public void testSetUp() {
    }

    @Test
    public void putKeyTest() throws Exception {
        Clique.getInstance().setTransport(new HttpTransport(_serviceUrl, _proxyUrl));
        Identity alice = Clique.getInstance().createIdentity(URI.create("uri:clique:alice"));
    }
}
