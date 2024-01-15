package io.github.awidesky.tcpCommunication;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import io.github.awidesky.tcpCommunication.client.Client;
import io.github.awidesky.tcpCommunication.server.EchoServer;
import io.github.awidesky.tcpCommunication.server.Server;

class Test {
	
	private static final int PORT = new Random().nextInt(49152, 65536);
	private static final int N = Runtime.getRuntime().availableProcessors() / 2;
	private static final ExecutorService threadPool = Executors.newFixedThreadPool(N);
	private static final Charset charset = StandardCharsets.UTF_8;
	
	@org.junit.jupiter.api.Test
	void test() throws IOException, InterruptedException {
		Server server = new EchoServer();
		server.open(PORT);
		
		final String IP = "localhost";//Server.getIP(); 
		
		List<Future<List<String>>> futures = new LinkedList<>();
		
		System.out.println("[Test] Testing with " + N + " clients...");
		for (int i = 0; i < N; i++) {
			final int n = i;
			futures.add(threadPool.submit(() -> {
				Client c = new Client();
				c.connect(IP, PORT);
				c.send((n + "Hello").getBytes(charset));
				c.send((n + "Bye").getBytes(charset));
				List<String> ret = new LinkedList<>();
				for(int j = 0; j < N*2; j++) ret.add(charset.decode(ByteBuffer.wrap(c.read())).toString());
				c.disconnect();
				return ret;
			}));

		}
		List<List<String>> results = futures.stream().map(t -> {
			try {
				return t.get(5000, TimeUnit.MILLISECONDS);
			} catch (InterruptedException | ExecutionException e) {
				return List.of(e.toString());
			} catch (TimeoutException e) {
				// TODO Auto-generated catch block
				return List.of(e.toString());
			}
		}).toList();

		server.close(5000);
		
		System.out.println("\n\nOutput of each clients:\n" + results.stream().map(s -> s.stream().collect(Collectors.joining("\n"))).collect(Collectors.joining("\n\n")) + "\nEnd of clients ouputs.\n\n");
		
		assertEquals(1, results.stream().map(HashSet::new).distinct().limit(2).count());
	}

}


/*
	threadPool.invokeAll(Arrays.stream(clients).map((c) -> {
			return new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					c.connect("localhost", PORT);
					return null;
				}
			};
		}).toList()).stream().forEach(t -> {
			try {
				t.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		});
 * */
