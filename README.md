# Clique Java HTTP Transport

## Building the SDK

```bash
$ mvn verify
```

## Artifacts and Reports

HTTP Library (package phase):
`target/clique-http-<version>.jar`

Unit Test Report (test phase):
`target/surefire-reports/index.html`

Test Coverage Report (test phase):
`target/site/jacoco-ut/index.html`

Static Analysis Report (verify phase):
`target/site/findbugs/findbugsXml.xml`

Coding Style Report (verify phase):
`target/checkstyle-result.xml`

Javadocs (verify phase):
`target/site/apidocs/index.html`

## Notes

Some java installations do not trust the Let's Encrypt CA.  To add the it to your java trust roots, execute the following from the project root directory:

```
sudo keytool \
    -import \
    -alias DSTRootCAX3 \
    -file "misc/DSTRootCAX3.crt" \
    -keystore $JAVA_HOME/jre/lib/security/cacerts \
    -storepass changeit
```

