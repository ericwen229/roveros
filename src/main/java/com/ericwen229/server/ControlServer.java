package com.ericwen229.server;

import com.ericwen229.node.PublisherNodeHandler;
import com.ericwen229.topic.TopicManager;
import geometry_msgs.Twist;
import lombok.NonNull;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.ros.namespace.GraphName;

import java.net.InetSocketAddress;
import java.util.Scanner;
import java.util.logging.Logger;

public class ControlServer extends WebSocketServer {

	private final ControlMsgPublisher msgPublisher = new ControlMsgPublisher(100);

	public ControlServer(@NonNull InetSocketAddress address) {
		super(address);
	}

	@Override
	public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
		Logger.getGlobal().warning(String.format("Control server connection established: %s", webSocket.getRemoteSocketAddress()));
	}

	@Override
	public void onClose(WebSocket webSocket, int i, String s, boolean b) {
		Logger.getGlobal().info(String.format("Control server connection closed: %s", webSocket.getRemoteSocketAddress()));
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
		Logger.getGlobal().warning(String.format("Control server exception: %s", e.getClass().getName()));
		webSocket.close();
	}

	@Override
	public void onStart() {
		Logger.getGlobal().info(String.format("Control server URI: %s", getAddress()));
	}

	private static class ControlMsgPublisher {

		private double linear = 0.0;
		private double angular = 0.0;
		private double linearScale = 1.0;
		private double angularScale = 1.0;

		private final Thread msgPublishThread;

		private ControlMsgPublisher(final long intervalMillis) {
			final PublisherNodeHandler<Twist> handler = TopicManager.publishOnTopic(GraphName.of("/cmd_vel_mux/input/teleop"), geometry_msgs.Twist.class);
			msgPublishThread = new Thread(() -> {
				handler.blockUntilReady();
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
