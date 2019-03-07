package com.ericwen229.server;

import com.ericwen229.node.RoverOSNode;
import com.ericwen229.server.message.request.ControlMsgModel;
import com.ericwen229.server.message.request.PoseEstimateMsgModel;
import com.ericwen229.server.message.request.RequestMsgModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import geometry_msgs.Twist;
import lombok.NonNull;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.ros.namespace.GraphName;
import org.ros.node.topic.Publisher;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

/**
 * This class implements a websocket server used for controlling Turtlebot.
 *
 * <p>Turtlebot control message consists of two parts: linear speed and angular speed.
 */
public class ControlServer extends WebSocketServer {

	/**
	 * Gson object used for message serialize and deserialize.
	 */
	private static final Gson gson;

	/**
	 * Object that fires control messages at a constant rate.
	 */
	private final ControlMsgPublisher msgPublisher;

	static {
		// make gson deserialize request to different types by checking out the specified field.
		RuntimeTypeAdapterFactory<RequestMsgModel> requestRuntimeTypeAdapterFactory
				= RuntimeTypeAdapterFactory
				.of(RequestMsgModel.class, RequestMsgModel.typeFieldName)
				.registerSubtype(PoseEstimateMsgModel.class, ControlMsgModel.typeFieldValue);
		gson = new GsonBuilder()
				.registerTypeAdapterFactory(requestRuntimeTypeAdapterFactory)
				.create();
	}

	/**
	 * Construct server with given address.
	 *
	 * @param address address to which server will listen
	 */
	public ControlServer(@NonNull RoverOSNode node, @NonNull InetSocketAddress address) {
		super(address);
		msgPublisher = new ControlMsgPublisher(node, 100);
	}

	@Override
	public void onStart() {
		Logger.getGlobal().info(
				String.format("RoverOS control server starting at %s", getAddress()));
	}

	@Override
	public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
		Logger.getGlobal().info(
				String.format("RoverOS control server established connection to %s", webSocket.getRemoteSocketAddress()));
	}

	@Override
	public void onClose(WebSocket webSocket, int i, String s, boolean b) {
		Logger.getGlobal().info(
				String.format("RoverOS control server closing connection to %s", webSocket.getRemoteSocketAddress()));
	}

	@Override
	public void onMessage(WebSocket webSocket, String s) {
		try {
			RequestMsgModel request = gson.fromJson(s, RequestMsgModel.class);
			if (request.getClass().equals(ControlMsgModel.class)) {
				doControl((ControlMsgModel)request);
			}
			else {
				Logger.getGlobal().warning(
						String.format(
								"RoverOS control server unhandled request type: %s. Dropping request %s from %s.",
								request.getClass(),
								s,
								webSocket.getRemoteSocketAddress()));
			}
		}
		catch (JsonSyntaxException e) {
			Logger.getGlobal().warning(
					String.format(
							"RoverOS control server invalid json syntax. Dropping Request %s from %s",
							s,
							webSocket.getRemoteSocketAddress()));
		}
	}

	@Override
	public void onError(WebSocket webSocket, Exception e) {
		Logger.getGlobal().severe(
				String.format("RoverOS control server exception: %s", e.getClass().getName()));
		Logger.getGlobal().warning(
				String.format("RoverOS control server about to drop connection to %s", webSocket.getRemoteSocketAddress()));
		webSocket.close();
	}

	/**
	 * Analyze control request and set linear and angular speed accordingly.
	 *
	 * @param request control request
	 */
	private void doControl(@NonNull ControlMsgModel request) {
		msgPublisher.setLinear(request.linear);
		msgPublisher.setAngular(request.angular);
	}

	/**
	 * This class implements publisher that publish control message to Turtlebot control topic
	 * at a constant rate.
	 */
	private class ControlMsgPublisher {

		/**
		 * Linear speed (forward or backward).
		 */
		private double linear = 0.0;

		/**
		 * Angular speed (left or right).
		 */
		private double angular = 0.0;

		/**
		 * Linear speed scale factor. Will be multiplied with linear speed to produce final linear speed.
		 */
		private double linearScale = 1.0;

		/**
		 * Angular speed scale factor. Will be multiplied with angular speed to produce final angular speed.
		 */
		private double angularScale = 1.0;

		/**
		 * Construct publisher firing messages at given rate.
		 *
		 * @param intervalMillis interval between two adjacent publishes
		 */
		private ControlMsgPublisher(@NonNull RoverOSNode node, final long intervalMillis) {
			final Publisher<Twist> publisher = node.publishOnTopic(GraphName.of("/cmd_vel_mux/input/teleop"), geometry_msgs.Twist.class);
			new Thread(() -> {
				while (!Thread.currentThread().isInterrupted()) {
					if (ControlServer.this.getConnections().size() > 0) {
						double linearValue, angularValue;
						synchronized (this) {
							linearValue = linear * linearScale;
							angularValue = angular * angularScale;
						}

						geometry_msgs.Twist msg = publisher.newMessage();
						msg.getLinear().setX(linearValue);
						msg.getAngular().setZ(angularValue);
						publisher.publish(msg);
					}

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
