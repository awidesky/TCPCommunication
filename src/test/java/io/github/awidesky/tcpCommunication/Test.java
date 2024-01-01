package io.github.awidesky.tcpCommunication;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.github.awidesky.tcpCommunication.client.Client;
import io.github.awidesky.tcpCommunication.server.Server;

class Test {
	
	private static final int PORT = 19132;
	private static final int N = Runtime.getRuntime().availableProcessors() * 4;
	private static final ExecutorService threadPool = Executors.newFixedThreadPool(N / 2);
	
	@org.junit.jupiter.api.Test
	void test() throws IOException, InterruptedException {
		Server server = new Server();
		server.connect(PORT);
		
		Client[] clients = new Client[N];
		List<Future<String>> futures = new LinkedList<>();
		
		for (int i = 0; i < N; i++) {
			clients[i] = new Client();
			futures.add(threadPool.submit(() -> {
				Client c = new Client();
				c.connect("localhost", PORT);
				return "";
			}));

		}

		fail("Not yet implemented");
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
