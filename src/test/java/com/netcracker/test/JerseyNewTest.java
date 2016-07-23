package com.netcracker.test;

import static javax.ws.rs.client.Entity.*;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.stream.IntStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.SslConfigurator;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

public class JerseyNewTest {
	
	private static final String URL = "https://localhost:8443/";
	
	private static KeyStore trustStore;
	
	@BeforeClass
	public static void setupTrustStore() throws Exception {
    	trustStore = KeyStore.getInstance("JKS");
    	trustStore.load(new FileInputStream("miko0516.trust.jks"), "miko0516".toCharArray());
	}

	@Test
	public void testMultipleRequestWithParallelStream() {
//		if this is uncommented, everything looks good!
//		HttpsURLConnection.getDefaultSSLSocketFactory();
		IntStream.range(0, 10)
			.parallel() // if this is commented, everything looks good!
			.mapToObj(i -> constructClient(trustStore).target(URL))
			.forEach(this::doRequest);
	}
	
	private static Client constructClient(KeyStore trustStore) {
		return ClientBuilder.newBuilder()
				.sslContext(newSslContext())
				.register(JacksonJsonProvider.class)
				.build();
	}

	private static SSLContext newSslContext() {
		return SslConfigurator.newInstance()
				.trustStore(trustStore)
				.createSSLContext();
	}
	
	private void doRequest(WebTarget webTarget) {
		try {
        	Response response = webTarget.request().post(entity("{\"requestContent\" : \"qweAsd\"}", MediaType.APPLICATION_JSON_TYPE));
        	System.out.println("\t" + response.getStatusInfo().getStatusCode() + " " + response.getStatusInfo().getReasonPhrase());
        	response.readEntity(String.class);
        	response.close();
    	} catch (Exception e) {
    		System.out.println("\t" + e.getMessage());
    	}
	}
	
}