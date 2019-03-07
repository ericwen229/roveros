package com.ericwen229.server;

import com.ericwen229.node.RoverOSNode;
import com.ericwen229.server.message.request.NavigationGoalMsgModel;
import com.ericwen229.server.message.request.PoseEstimateMsgModel;
import com.ericwen229.server.message.request.RequestMsgModel;
import com.ericwen229.server.message.response.PoseMsgModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import geometry_msgs.Point;
import geometry_msgs.PoseStamped;
import geometry_msgs.PoseWithCovarianceStamped;
import geometry_msgs.Quaternion;
import lombok.NonNull;
import nav_msgs.MapMetaData;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.ros.namespace.GraphName;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

/**
 * This class implements a websocket server used for navigating Turtlebot.
 *
 * <p>The server expects two types of requests: pose estimation and navigation goal.
 * Pose estimation (topic /initialpose) tells Turtlebot its approximate pose, which
 * is required before navigating the bot. Navigation goal (topic /move_base_simple)
 * tells Turtlebot where to go. The navigation functionality is implemented in amcl
 * ROS package.
 *
 * <p>Also, the server retrieves map meta data from ROS system (topic /map_metadata).
 * Requests are translated using the map meta data before being published.
 */
public class NavigationServer extends WebSocketServer {

	/**
	 * Gson object used for message serialize and deserialize.
	 */
	private static final Gson gson;

	/**
	 * ROS node used by RoverOS
	 */
	private final RoverOSNode node;

	/**
	 * This object encapsulates navigation functions.
	 */
	private final NavigationManager navigationManager;

	static {
		// make gson deserialize request to different types by checking out the specified field.
		RuntimeTypeAdapterFactory<RequestMsgModel> requestRuntimeTypeAdapterFactory
				= RuntimeTypeAdapterFactory
				.of(RequestMsgModel.class, RequestMsgModel.typeFieldName)
				.registerSubtype(PoseEstimateMsgModel.class, PoseEstimateMsgModel.typeFieldValue)
				.registerSubtype(NavigationGoalMsgModel.class, NavigationGoalMsgModel.typeFieldValue);
		gson = new GsonBuilder()
				.registerTypeAdapterFactory(requestRuntimeTypeAdapterFactory)
				.create();
	}

	/**
	 * Create server with given ROS node and address.
	 *
	 * @param address address to which server will listen
	 */
	public NavigationServer(@NonNull RoverOSNode node, @NonNull InetSocketAddress address) {
		super(address);
		this.node = node;
		this.navigationManager = new NavigationManager(node);
	}

	@Override
	public void onStart() {
		Logger.getGlobal().info(
				String.format("RoverOS navigation server starting at %s", getAddress()));
	}

	@Override
	public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
		Logger.getGlobal().info(
				String.format("RoverOS navigation server established connection to %s", webSocket.getRemoteSocketAddress()));
	}

	@Override
	public void onClose(WebSocket webSocket, int i, String s, boolean b) {
		Logger.getGlobal().info(
				String.format("RoverOS navigation server closing connection to %s", webSocket.getRemoteSocketAddress()));
	}

	@Override
	public void onMessage(WebSocket webSocket, String s) {
		try {
			RequestMsgModel request = gson.fromJson(s, RequestMsgModel.class);
			if (request.getClass().equals(PoseEstimateMsgModel.class)) {
				navigationManager.doPoseEstimate((PoseEstimateMsgModel) request);
			}
			else if (request.getClass().equals(NavigationGoalMsgModel.class)) {
				navigationManager.doNavigationGoal((NavigationGoalMsgModel) request);
			}
			else {
				Logger.getGlobal().warning(
						String.format(
								"Unhandled request type: %s. Dropping request %s from %s.",
								request.getClass(),
								s,
								webSocket.getRemoteSocketAddress()));
			}
		}
		catch (JsonSyntaxException e) {
			Logger.getGlobal().warning(
					String.format(
							"RoverOS navigation server invalid json syntax. Dropping Request %s from %s",
							s,
							webSocket.getRemoteSocketAddress()));
		}
	}

	@Override
	public void onError(WebSocket webSocket, Exception e) {
		Logger.getGlobal().severe(
				String.format("RoverOS navigation server exception: %s", e.getClass().getName()));
		Logger.getGlobal().warning(
				String.format("RoverOS navigation server about to drop connection to %s", webSocket.getRemoteSocketAddress()));
		webSocket.close();
	}

	/**
	 * This class encapsulates navigation related functions, like map metadata management,
	 * message publishing and pose monitoring.
	 */
	private class NavigationManager {

		/**
		 * Publisher used to publish estimated pose.
		 */
		private final Publisher<PoseWithCovarianceStamped> poseEstimatePublisher;

		/**
		 * Publisher used to publish navigation goal.
		 */
		private final Publisher<PoseStamped> navigationGoalPublisher;

		/**
		 * Subscriber used to retrieve map meta data.
		 */
		private final Subscriber<MapMetaData> mapMetaDataSubscriber;

		/**
		 * Subscriber used to retrieve real time pose.
		 */
		private final Subscriber<PoseWithCovarianceStamped> poseSubscriber;

		/**
		 * Mutex of accessing map meta data.
		 */
		private final Object mapMetaDataMutex = new Object();

		/**
		 * True if map meta data is received.
		 */
		private boolean isMapMetaDataLoaded = false;

		/**
		 * Width of map.
		 */
		private int mapWidth;

		/**
		 * Height of map.
		 */
		private int mapHeight;

		/**
		 * Coordinate X of origin of map.
		 */
		private double originX;

		/**
		 * Coordinate Y of origin of map.
		 */
		private double originY;

		/**
		 * Resolution of map.
		 */
		private double resolution;

		/**
		 * Default constructor that creates publishers & subscribers.
		 */
		private NavigationManager(@NonNull RoverOSNode node) {
			poseEstimatePublisher = node.publishOnTopic(GraphName.of("/initialpose"), PoseWithCovarianceStamped.class);
			navigationGoalPublisher = node.publishOnTopic(GraphName.of("/move_base_simple/goal"), PoseStamped.class);
			mapMetaDataSubscriber = node.subscribeToTopic(GraphName.of("/map_metadata"), MapMetaData.class);
			poseSubscriber = node.subscribeToTopic(GraphName.of("/amcl_pose"), PoseWithCovarianceStamped.class);

			mapMetaDataSubscriber.addMessageListener(this::handleMapMetaData);
			poseSubscriber.addMessageListener(this::handlePose);
		}

		/**
		 * Analyze pose estimate request and publish a message to do pose estimate
		 *
		 * @param request pose estimate request
		 */
		private void doPoseEstimate(@NonNull PoseEstimateMsgModel request) {
			if (!isMapMetaDataLoaded) {
				Logger.getGlobal().warning(
						"RoverOS navigation server map metadata not ready. Dropping request.");
				return;
			}

			PoseWithCovarianceStamped msg = poseEstimatePublisher.newMessage();
			msg.getHeader().setStamp(node.getCurrentTime());
			msg.getHeader().setFrameId("map");

			Point position = msg.getPose().getPose().getPosition();
			synchronized (mapMetaDataMutex) {
				position.setX(originX + request.x * mapWidth * resolution);
				position.setY(originY + request.y * mapHeight * resolution);
			}

			Quaternion orientation = msg.getPose().getPose().getOrientation();
			double angleRad = request.angle * Math.PI;
			orientation.setZ(Math.sin(angleRad));
			orientation.setW(Math.cos(angleRad));

			poseEstimatePublisher.publish(msg);
		}

		/**
		 * Analyze navigation goal request and publish a message to specify navigation goal.
		 *
		 * @param request navigation goal request
		 */
		private void doNavigationGoal(@NonNull NavigationGoalMsgModel request) {
			if (!isMapMetaDataLoaded) {
				Logger.getGlobal().warning(
						"RoverOS navigation server map metadata not ready. Dropping request.");
				return;
			}

			PoseStamped msg = navigationGoalPublisher.newMessage();
			msg.getHeader().setStamp(node.getCurrentTime());
			msg.getHeader().setFrameId("map");

			Point position = msg.getPose().getPosition();
			synchronized (mapMetaDataMutex) {
				position.setX(originX + request.x * mapWidth * resolution);
				position.setY(originY + request.y * mapHeight * resolution);
			}

			Quaternion orientation = msg.getPose().getOrientation();
			double angleRad = request.angle * Math.PI;
			orientation.setZ(Math.sin(angleRad));
			orientation.setW(Math.cos(angleRad));

			navigationGoalPublisher.publish(msg);
		}

		/**
		 * Callback invoked when map meta data is received.
		 *
		 * @param message received map meta data
		 */
		private void handleMapMetaData(@NonNull MapMetaData message) {
			synchronized (mapMetaDataMutex) {
				isMapMetaDataLoaded = true;
				mapWidth = message.getWidth();
				mapHeight = message.getHeight();
				originX = message.getOrigin().getPosition().getX();
				originY = message.getOrigin().getPosition().getY();
				resolution = message.getResolution();
				Logger.getGlobal().info(
						String.format(
								"RoverOS navigation server map metadata in position: w%d h%d x%f y%f r%f",
								mapWidth,
								mapHeight,
								originX,
								originY,
								resolution));
			}
		}

		/**
		 * Callback when pose is received.
		 *
		 * @param message received pose
		 */
		private void handlePose(@NonNull PoseWithCovarianceStamped message) {
			Point position = message.getPose().getPose().getPosition();
			// TODO: translate orientation quaternion to angle
			Quaternion orientation = message.getPose().getPose().getOrientation();

			PoseMsgModel msg = new PoseMsgModel();
			msg.x = position.getX();
			msg.y = position.getY();
			broadcast(gson.toJson(msg));
		}

	}
}
