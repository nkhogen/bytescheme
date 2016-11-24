package com.bytescheme.rpc.core;

import java.util.UUID;

import com.google.gson.Gson;

public class TestClient {
	public interface HelloProcessor extends RemoteObject {
		HelloProcessor hello();

		String hello(String name);

		int changePowerState(int deviceId, int state);
	}

	public static class HelloProcessImpl implements HelloProcessor {
		public UUID objectId = new UUID(0L, 1L);

		@Override
		public UUID getObjectId() {
			return objectId;
		}

		@Override
		public HelloProcessor hello() {
			HelloProcessImpl impl = new HelloProcessImpl();
			impl.objectId = new UUID(0L, 2L);
			return impl;
		}

		@Override
		public String hello(String name) {
			return "Hello " + name + "! How are you?";
		}

		@Override
		public int changePowerState(int deviceId, int state) {
			return state;
		}
	}

	public static void main(String[] args) throws Exception {
		RemoteObjectClientBuilder clientBuilder = new RemoteObjectClientBuilder("http://127.0.0.1:8080/rpc");
		RemoteObjectClient client = clientBuilder.login("nkhogen", "testpassword");
		try {
			TestClient.HelloProcessor processor = client.createRemoteObject(TestClient.HelloProcessor.class,
					new UUID(0L, 1L));
			System.out.println(processor.hello());
			System.out.println(processor.hello("Khogen") + new UUID(0L, 2L));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			client.logout();
		}
	}
}
