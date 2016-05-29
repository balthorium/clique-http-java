package com.cisco.clique.sdk;

import com.cisco.clique.http.HttpTransport;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.net.URI;
import java.security.Security;

public class HttpTransportTest {

    @BeforeTest
    public void suiteSetUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @BeforeMethod
    public void testSetUp() {
    }

    @Test
    public void putKeyTest() throws Exception {

        Clique.getInstance().setTransport(new HttpTransport());
        Identity alice = Clique.getInstance().createIdentity(URI.create("uri:clique:alice"));
    }
}
