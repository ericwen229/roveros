package com.ericwen229.server;

import com.ericwen229.node.NodeExecutor;
import lombok.NonNull;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.ros.exception.RosRuntimeException;

import java.net.InetSocketAddress;
import java.util.Scanner;

public class ControlServer extends WebSocketServer {

	// ========== members ==========

	private final ControlMsgPublisher msgPublisher = new ControlMsgPublisher(100);

	// ========== constructor ==========

	public ControlServer(@NonNull InetSocketAddress address) {
		super(address);
		System.out.println(String.format("[control server starting on %s:%d]", address.getHostName(), address.getPort()));
	}

	// ========== overridden methods ==========

	@Override
	public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
		// TODO: client connection handling
		System.out.println("[a client just came to control server]");
	}

	@Override
	public void onClose(WebSocket webSocket, int i, String s, boolean b) {
		// TODO: client removal handling
		System.out.println("[a client has left control server]");
	}

	@Override
	public void onMessage(WebSocket webSocket, String s) {
		// TODO: protocol design
		Scanner scanner = new Scanner(s);
		double linear = scanner.nextDouble();
		double angular = scanner.nextDouble();

		msgPublisher.setLinear(linear);
		msgPublisher.setAngular(angular);
	}

	@Override
	public void onError(WebSocket webSocket, Exception e) {
		// TODO: error handling
		System.out.println(String.format("[control server exception: %s]", e.toString()));
	}

	@Override
	public void onStart() {
		// TODO: server startup
		System.out.println("[control server is up]");
	}

	// ========== util classes ==========

	private static class ControlMsgPublisher {

		private double linear = 0.0;
		private double angular = 0.0;
		private double linearScale = 1.0;
		private double angularScale = 1.0;

		private final Thread msgPublishThread;

		private ControlMsgPublisher(final long intervalMillis) {
			msgPublishThread = new Thread(new Runnable() {
				public void run() {
					while (!Thread.currentThread().isInterrupted()) {
						double linearValue, angularValue;
						synchronized (this) {
							linearValue = linear * linearScale;
							angularValue = angular * angularScale;
						}

						try {
							NodeExecutor.acquireControllerNode().publish(linearValue, angularValue);
						} catch (RosRuntimeException e) {
							System.out.println("[control message publish failed]");
						}

						try {
							Thread.sleep(intervalMillis);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
					}
				}
			});
			msgPublishThread.start();
		}

		private void setLinear(double value) {
			if (Math.abs(value) > 1.0) {
				// TODO: exception
				throw new RuntimeException();
			}

			synchronized (this) {
				linear = value;
			}
		}

		private void setAngular(double value) {
			if (Math.abs(value) > 1.0) {
				// TODO: exception
				throw new RuntimeException();
			}

			synchronized (this) {
				angular = value;
			}
		}

		private void setLinearScale(double value) {
			synchronized (this) {
				linearScale = value;
			}
		}

		private void setAngularScale(double value) {
			synchronized (this) {
				angularScale = value;
			}
		}

	}

}
