package com.ericwen229.server;

import com.ericwen229.topic.PublisherHandler;
import com.ericwen229.topic.TopicManager;
import lombok.NonNull;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.ros.namespace.GraphName;

import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.logging.Logger;

public class ControlServer extends WebSocketServer {

	private static final Logger logger = Logger.getLogger(ControlServer.class.getName());

	// ========== members ==========

	private final ControlMsgPublisher msgPublisher = new ControlMsgPublisher(100);

	// ========== constructor ==========

	public ControlServer(@NonNull InetSocketAddress address) {
		super(address);
	}

	// ========== overridden methods ==========

	@Override
	public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
		logger.warning(String.format("Control server connection established: %s", webSocket.getRemoteSocketAddress()));
	}

	@Override
	public void onClose(WebSocket webSocket, int i, String s, boolean b) {
		logger.info(String.format("Control server connection closed: %s", webSocket.getRemoteSocketAddress()));
	}

	@Override
	public void onMessage(WebSocket webSocket, String s) {
		Scanner scanner = new Scanner(s);
		double linear = scanner.nextDouble();
		double angular = scanner.nextDouble();

		msgPublisher.setLinear(linear);
		msgPublisher.setAngular(angular);
	}

	@Override
	public void onError(WebSocket webSocket, Exception e) {
		logger.warning(String.format("Control server exception: %s", e.getClass().getName()));
		webSocket.close();
	}

	@Override
	public void onStart() {
		logger.info(String.format("Control server URI: %s", getAddress()));
	}

	// ========== util classes ==========

	private static class ControlMsgPublisher {

		private double linear = 0.0;
		private double angular = 0.0;
		private double linearScale = 1.0;
		private double angularScale = 1.0;

		private final Thread msgPublishThread;

		private ControlMsgPublisher(final long intervalMillis) {
			final PublisherHandler<geometry_msgs.Twist> handler = TopicManager.createPublisherHandler(GraphName.of("/cmd_vel_mux/input/teleop"), geometry_msgs.Twist.class);
			msgPublishThread = new Thread(new Runnable() {
				public void run() {
					while (!Thread.currentThread().isInterrupted()) {
						double linearValue, angularValue;
						synchronized (this) {
							linearValue = linear * linearScale;
							angularValue = angular * angularScale;
						}

						geometry_msgs.Twist msg = handler.newMessage();
						msg.getLinear().setX(linearValue);
						msg.getAngular().setZ(angularValue);
						handler.publish(msg);

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
			synchronized (this) {
				linear = value;
			}
		}

		private void setAngular(double value) {
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
