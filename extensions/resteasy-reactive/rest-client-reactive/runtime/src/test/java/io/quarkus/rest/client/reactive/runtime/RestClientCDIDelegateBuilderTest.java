package io.quarkus.rest.client.reactive.runtime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.quarkus.restclient.config.RestClientConfig;
import io.quarkus.restclient.config.RestClientsConfig;

@SuppressWarnings({ "SameParameterValue" })
public class RestClientCDIDelegateBuilderTest {

    private static final String TRUSTSTORE_PASSWORD = "truststorePassword";
    private static final String KEYSTORE_PASSWORD = "keystorePassword";

    @TempDir
    static File tempDir;
    private static File truststoreFile;
    private static File keystoreFile;
    private static Config createdConfig;

    @BeforeAll
    public static void beforeAll() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        // prepare keystore and truststore

        truststoreFile = new File(tempDir, "truststore.jks");
        keystoreFile = new File(tempDir, "keystore.jks");

        KeyStore truststore = KeyStore.getInstance("JKS");
        truststore.load(null, TRUSTSTORE_PASSWORD.toCharArray());
        truststore.store(new FileOutputStream(truststoreFile), TRUSTSTORE_PASSWORD.toCharArray());

        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(null, KEYSTORE_PASSWORD.toCharArray());
        keystore.store(new FileOutputStream(keystoreFile), KEYSTORE_PASSWORD.toCharArray());
    }

    @AfterEach
    public void afterEach() {
        if (createdConfig != null) {
            ConfigProviderResolver.instance().releaseConfig(createdConfig);
            createdConfig = null;
        }
    }

    @Test
    public void testQuarkusConfig() {
        RestClientsConfig configRoot = createSampleConfiguration();
        RestClientBuilderImpl restClientBuilderMock = Mockito.mock(RestClientBuilderImpl.class);
        new RestClientCDIDelegateBuilder<>(TestClient.class,
                "http://localhost:8080",
                "test-client",
                configRoot).build(restClientBuilderMock);

        Mockito.verify(restClientBuilderMock).baseUri(URI.create("http://localhost"));
        Mockito.verify(restClientBuilderMock).register(MyResponseFilter1.class);
        Mockito.verify(restClientBuilderMock).connectTimeout(100, TimeUnit.MILLISECONDS);
        Mockito.verify(restClientBuilderMock).readTimeout(101, TimeUnit.MILLISECONDS);
        Mockito.verify(restClientBuilderMock).followRedirects(true);
        Mockito.verify(restClientBuilderMock).proxyAddress("localhost", 1234);
        Mockito.verify(restClientBuilderMock).queryParamStyle(QueryParamStyle.COMMA_SEPARATED);
        Mockito.verify(restClientBuilderMock).trustStore(Mockito.any());
        Mockito.verify(restClientBuilderMock).keyStore(Mockito.any(), Mockito.anyString());
        Mockito.verify(restClientBuilderMock).hostnameVerifier(Mockito.any(MyHostnameVerifier1.class));
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.CONNECTION_TTL, 30); // value in seconds
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.CONNECTION_POOL_SIZE, 103);
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.MAX_REDIRECTS, 104);
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.SHARED, true);
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.NAME, "my-client");
        Mockito.verify(restClientBuilderMock).property(QuarkusRestClientProperties.MULTIPART_ENCODER_MODE,
                HttpPostRequestEncoder.EncoderMode.HTML5);
    }

    @Test
    public void testGlobalTimeouts() {
        RestClientsConfig configRoot = new RestClientsConfig();
        configRoot.connectTimeout = 5000L;
        configRoot.readTimeout = 10000L;
        configRoot.multipartPostEncoderMode = Optional.empty();
        configRoot.userAgent = Optional.empty();
        RestClientBuilderImpl restClientBuilderMock = Mockito.mock(RestClientBuilderImpl.class);
        new RestClientCDIDelegateBuilder<>(TestClient.class,
                "http://localhost:8080",
                "test-client",
                configRoot).build(restClientBuilderMock);

        Mockito.verify(restClientBuilderMock).connectTimeout(5000, TimeUnit.MILLISECONDS);
        Mockito.verify(restClientBuilderMock).readTimeout(10000, TimeUnit.MILLISECONDS);
    }

    private static RestClientsConfig createSampleConfiguration() {
        RestClientConfig clientConfig = new RestClientConfig();
        clientConfig.url = Optional.of("http://localhost");
        clientConfig.uri = Optional.empty();
        clientConfig.scope = Optional.of("Singleton");
        clientConfig.providers = Optional
                .of("io.quarkus.rest.client.reactive.runtime.RestClientCDIDelegateBuilderTest$MyResponseFilter1");
        clientConfig.connectTimeout = Optional.of(100L);
        clientConfig.readTimeout = Optional.of(101L);
        clientConfig.followRedirects = Optional.of(true);
        clientConfig.proxyAddress = Optional.of("localhost:1234");
        clientConfig.proxyUser = Optional.of("admin");
        clientConfig.proxyPassword = Optional.of("adm1n");
        clientConfig.nonProxyHosts = Optional.empty();
        clientConfig.queryParamStyle = Optional.of(QueryParamStyle.COMMA_SEPARATED);
        clientConfig.trustStore = Optional.of(truststoreFile.getAbsolutePath());
        clientConfig.trustStorePassword = Optional.of("truststorePassword");
        clientConfig.trustStoreType = Optional.of("JKS");
        clientConfig.keyStore = Optional.of(keystoreFile.getAbsolutePath());
        clientConfig.keyStorePassword = Optional.of("keystorePassword");
        clientConfig.keyStoreType = Optional.of("JKS");
        clientConfig.hostnameVerifier = Optional
                .of("io.quarkus.rest.client.reactive.runtime.RestClientCDIDelegateBuilderTest$MyHostnameVerifier1");
        clientConfig.connectionTTL = Optional.of(30000); // value in milliseconds
        clientConfig.connectionPoolSize = Optional.of(103);
        clientConfig.maxRedirects = Optional.of(104);
        clientConfig.headers = Collections.emptyMap();
        clientConfig.shared = Optional.of(true);
        clientConfig.name = Optional.of("my-client");
        clientConfig.userAgent = Optional.of("rest-client");

        RestClientsConfig configRoot = new RestClientsConfig();
        configRoot.multipartPostEncoderMode = Optional.of("HTML5");
        configRoot.disableSmartProduces = Optional.of(true);
        configRoot.putClientConfig("test-client", clientConfig);

        return configRoot;
    }

    @RegisterRestClient(configKey = "test-client")
    interface TestClient {
        @SuppressWarnings("unused")
        String methodA();
    }

    public static class MyResponseFilter1 implements ClientResponseFilter {
        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        }
    }

    public static class MyResponseFilter2 implements ClientResponseFilter {
        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) {
        }
    }

    public static class MyHostnameVerifier1 implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    public static class MyHostnameVerifier2 implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

}
