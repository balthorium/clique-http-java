package com.cisco.clique.http.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.Base64URL;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicKey {

    private String _kid;
    private String _crv;
    private String _kty;
    private String _x;
    private String _y;

    public PublicKey() {
    }

    public PublicKey(ECKey key) throws Exception {
        _kid = key.getKeyID();
        _crv = key.getCurve().getName();
        _kty = key.getKeyType().getValue();
        _x = key.getX().toString();
        _y = key.getY().toString();

        if (null == _kid) {
            _kid = key.computeThumbprint().toString();
        }
    }

    public String getKid() {
        return _kid;
    }

    public void setKid(String kid) {
        _kid = kid;
    }

    public String getCrv() {
        return _crv;
    }

    public void setCrv(String crv) {
        _crv = crv;
    }

    public String getKty() {
        return _kty;
    }

    public void setKty(String kty) {
        _kty = kty;
    }

    public String getX() {
        return _x;
    }

    public void setX(String x) {
        _x = x;
    }

    public String getY() {
        return _y;
    }

    public void setY(String y) {
        _y = y;
    }

    @JsonIgnore
    public ECKey getKey() {
        return new ECKey.Builder(ECKey.Curve.parse(_crv), new Base64URL(_x), new Base64URL(_y))
                .keyID(_kid)
                .build();
    }
}
