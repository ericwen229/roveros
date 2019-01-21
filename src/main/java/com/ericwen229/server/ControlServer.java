package com.ericwen229.server;

import com.ericwen229.node.TopicPublishHandler;
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

/**
 * This class implements a websocket server used for controlling Turtlebot.
 *
 * <p>Turtlebot control message consists of two parts: linear speed and angular speed.
 * Thus the websocket server simply interprets a request as two numbers indicating
 * the linear speed and angular speed.
 */
public class ControlServer extends WebSocketServer {

	/**
	 * Object that fires control messages at a constant rate.
	 */
	private final ControlMsgPublisher msgPublisher = new ControlMsgPublisher(100);

	/**
	 * Construct server with given address.
	 *
	 * @param address address to which server will listen
	 */
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

	/**
	 * This class implements publisher that publish control message to Turtlebot control topic
	 * at a constant rate.
	 */
	private static class ControlMsgPublisher {

		/**
		 * Linear speed (forward or backward).
		 */
		private volatile double linear = 0.0;

		/**
		 * Angular speed (left or right).
		 */
		private volatile double angular = 0.0;

		/**
		 * Linear speed scale factor. Will be multiplied with linear speed to produce final linear speed.
		 */
		private volatile double linearScale = 1.0;

		/**
		 * Angular speed scale factor. Will be multiplied with angular speed to produce final angular speed.
		 */
		private volatile double angularScale = 1.0;

		/**
		 * Construct publisher firing messages at given rate.
		 *
		 * @param intervalMillis interval between two adjacent publishes
		 */
		private ControlMsgPublisher(final long intervalMillis) {
			final TopicPublishHandler<Twist> handler = TopicManager.publishOnTopic(GraphName.of("/cmd_vel_mux/input/teleop"), geometry_msgs.Twist.class);
			new Thread(() -> {
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
			}).start();
		}

		/**
		 * Set linear speed.
		 *
		 * @param value linear speed value
		 */
		private synchronized void setLinear(double value) {
			linear = value;
		}

		/**
		 * Set angular speed.
		 *
		 * @param value angular speed value
		 */
		private synchronized void setAngular(double value) {
			angular = value;
		}

		/**
		 * Set linear speed scale factor.
		 *
		 * @param value linear speed scale factor value
		 */
		private synchronized void setLinearScale(double value) {
			linearScale = value;
		}

		/**
		 * Set angular speed scale factor.
		 *
		 * @param value angular speed scale factor value
		 */
		private synchronized void setAngularScale(double value) {
			angularScale = value;
		}

	}

}
