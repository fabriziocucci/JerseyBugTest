package com.netcracker.test;

import static javax.ws.rs.client.Entity.entity;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.SslConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

public class JerseyNewTest {
	
	private static final Object mutex = new Object();
	private static final AtomicInteger count = new AtomicInteger(0);
	private static final String URL = "https://localhost:8443/";
	
	private static KeyStore trustStore;

	private Client client;
	
	@BeforeClass
	public static void setupTrustStore() throws Exception {
    	trustStore = KeyStore.getInstance("JKS");
    	trustStore.load(new FileInputStream("miko0516.trust.jks"), "miko0516".toCharArray());
	}

	@Before
	public void setupClient() {
    	client = constructClient(trustStore);
    	count.set(0);
	}
	
	@After
	public void closeClient() {
		client.close();
	}
	
	@Test
	@Ignore
	public void testSingle() throws Exception {
		System.out.println("START single TEST");
        WebTarget target = client.target(URL);
        Builder builder = target.request();
    	Response response = builder.post(entity("{\"requestContent\" : \"qweAsd\"}", MediaType.APPLICATION_JSON_TYPE));
    	assert response != null;
    	System.out.println(response.getStatusInfo().getStatusCode() + " " + response.getStatusInfo().getReasonPhrase());
    	response.readEntity(String.class);
    	response.close();
    	System.out.println("FINSH single TEST");
	}

	@Test
	@Ignore
	public void testMultithread3() throws Exception {
		testMultithread(3);
	}
	
	@Test
	public void testMultithread10() throws Exception {
		testMultithread(10);
	}
	
	@Test
	@Ignore
	public void testMultithread50() throws Exception {
		testMultithread(50);
	}
	
	public void testMultithread(int size) throws Exception {
    	System.out.println("START with " + size + " THREADS");
		for (int i = 0; i < size; i++) {
        	new Thread(new RunnerThread(client)).start();
        }
        synchronized (mutex) {
        	while (count.get() != size) {
        		mutex.wait(10);
        	}
        	mutex.notifyAll(); // let's start!
		}
        synchronized (mutex) {
        	while (count.get() != 0) {
        		mutex.wait(10);
        	}
		}
    	System.out.println("ALL " + size + " FINISHED");
	}

	protected static Client constructClient(KeyStore trustStore) {
		
		SslConfigurator sslConfig = SslConfigurator.newInstance().trustStore(trustStore);
		SSLContext sslContext = sslConfig.createSSLContext();

		
		ClientBuilder builder = ClientBuilder.newBuilder();
    	builder.sslContext(sslContext);
    	
    	Client client = builder.build();
    	client.register(JacksonJsonProvider.class);
    	return client;
	}

    static class RunnerThread implements Runnable {
		final Client client;
		private Builder builder;
		private String name;
    	
    	public RunnerThread(Client client) {
    		this.client = client;
		}
    	
    	@Override
    	public void run() {
    		name = Thread.currentThread().getName();
	        WebTarget target = client.target(URL);
	        builder = target.request();
	        synchronized (mutex) {
		        count.incrementAndGet();
	        	try {
					mutex.wait();
				} catch (InterruptedException e) {
				}
			}
			doRequest();
    		count.decrementAndGet();
    		synchronized (mutex) {
    			mutex.notify();
			}
    	}

		protected void doRequest() {
			try {
	        	Response response = builder.post(entity("{\"requestContent\" : \"qweAsd\"}", MediaType.APPLICATION_JSON_TYPE));
	        	assert response != null;
	        	System.out.println(name + "\t" + response.getStatusInfo().getStatusCode() + " " + response.getStatusInfo().getReasonPhrase());
	        	response.readEntity(String.class);
	        	response.close();
        	} catch (Exception e) {
        		System.out.println(name + "\t" + e.getMessage());
        	}
		}
    }
}