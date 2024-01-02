package io.github.awidesky.tcpCommunication;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import io.github.awidesky.tcpCommunication.client.Client;
import io.github.awidesky.tcpCommunication.server.EchoServer;
import io.github.awidesky.tcpCommunication.server.Server;

class Test {
	
	private static final int PORT = 19132;
	private static final int N = Runtime.getRuntime().availableProcessors() * 4;
	private static final ExecutorService threadPool = Executors.newFixedThreadPool(N / 2);
	private static final Charset charset = StandardCharsets.UTF_8;
	
	@org.junit.jupiter.api.Test
	void test() throws IOException, InterruptedException {
		Server server = new EchoServer();
		server.open(PORT);
		
		Client[] clients = new Client[N];
		List<Future<Set<String>>> futures = new LinkedList<>();
		
		for (int i = 0; i < N; i++) {
			final int n = i;
			clients[n] = new Client();
			futures.add(threadPool.submit(() -> {
				Client c = new Client();
				c.connect("localhost", PORT);
				c.send((n + "Hello").getBytes(charset));
				c.send((n + "Bye").getBytes(charset));
				c.disconnect();
				byte[] b;
				Set<String> ret = new LinkedHashSet<>();
				while((b = c.read()) != null) ret.add(charset.decode(ByteBuffer.wrap(b)).toString());
				return ret;
			}));

		}
		List<Set<String>> results = futures.stream().map(t -> {
			try {
				return t.get();
			} catch (InterruptedException | ExecutionException e) {
				return Set.of("");
			}
		}).toList();

		results.stream().map(s -> s.stream().collect(Collectors.joining("\n"))).forEach(System.out::print);
		
		assertEquals(1, results.stream().distinct().limit(2).count());
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
