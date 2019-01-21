package com.ericwen229.server;

import com.ericwen229.node.NodeManager;
import com.ericwen229.node.TopicPublishHandler;
import com.ericwen229.node.TopicSubscribeHandler;
import com.ericwen229.server.message.request.NavigationGoalMsgModel;
import com.ericwen229.server.message.request.PoseEstimateMsgModel;
import com.ericwen229.server.message.request.RequestMsgModel;
import com.ericwen229.topic.TopicManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import geometry_msgs.Point;
import geometry_msgs.PoseStamped;
import geometry_msgs.PoseWithCovarianceStamped;
import geometry_msgs.Quaternion;
import lombok.NonNull;
import nav_msgs.MapMetaData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.ros.namespace.GraphName;

import java.net.InetSocketAddress;

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
	 * Logger.
	 */
	private static final Log logger = LogFactory.getLog(NavigationServer.class);

	/**
	 * This object encapsulates navigation functions.
	 */
	private final NavigationManager navigationManager;

	static {
		// make Gson deserialize to different types by checking out the specified field.
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
	 * Construct server with given address.
	 *
	 * @param address address to which server will listen
	 */
	public NavigationServer(@NonNull InetSocketAddress address) {
		super(address);
		navigationManager = new NavigationManager();
	}

	@Override
	public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
		logger.info(String.format("RoverOS server connection established: %s", webSocket.getRemoteSocketAddress()));
	}

	@Override
	public void onClose(WebSocket webSocket, int i, String s, boolean b) {
		logger.info(String.format("RoverOS server connection closed: %s", webSocket.getRemoteSocketAddress()));
	}

	@Override
	public void onMessage(WebSocket webSocket, String s) {
		RequestMsgModel request = gson.fromJson(s, RequestMsgModel.class);
		if (request.getClass().equals(PoseEstimateMsgModel.class)) {
			navigationManager.doPoseEstimate((PoseEstimateMsgModel)request);
		}
		else if (request.getClass().equals(NavigationGoalMsgModel.class)) {
			navigationManager.doNavigationGoal((NavigationGoalMsgModel)request);
		}
		else {
			logger.warn(String.format("Unhandled request type %s. Dropping request.",request.getClass().toString()));
		}
	}

	@Override
	public void onError(WebSocket webSocket, Exception e) {
		logger.error(String.format("RoverOS server exception: %s", e.getClass().getName()));
		webSocket.close();
	}

	@Override
	public void onStart() {
		logger.info(String.format("RoverOS server starting: %s", getAddress()));
	}

	/**
	 * This class encapsulates navigation related functions, like map metadata management,
	 * message publishing and pose monitoring.
	 */
	private class NavigationManager {

		/**
		 * Publish handler used to publish estimated pose.
		 */
		private final TopicPublishHandler<PoseWithCovarianceStamped> poseEstimatePublishHandler;

		/**
		 * Publish handler used to publish navigation goal.
		 */
		private final TopicPublishHandler<PoseStamped> goalPublishHandler;

		/**
		 * Subscribe handler used to retrieve map meta data.
		 */
		private final TopicSubscribeHandler<MapMetaData> mapMetaDataSubscribeHandler;

		/**
		 * Subscribe handler used to retrieve real time pose.
		 */
		private final TopicSubscribeHandler<PoseWithCovarianceStamped> poseSubscribeHandler;

		/**
		 * Mutex of accessing map meta data.
		 */
		private final Object mapMetaDataMutex = new Object();

		/**
		 * True if map meta data is received.
		 */
		private volatile boolean isMapMetaDataLoaded = false;

		/**
		 * Width of map.
		 */
		private volatile int mapWidth;

		/**
		 * Height of map.
		 */
		private volatile int mapHeight;

		/**
		 * Coordinate X of origin of map.
		 */
		private volatile double originX;

		/**
		 * Coordinate Y of origin of map.
		 */
		private volatile double originY;

		/**
		 * Resolution of map.
		 */
		private volatile double resolution;

		/**
		 * Default constructor that creates publish & subscribe handlers.
		 */
		private NavigationManager() {
			poseEstimatePublishHandler = TopicManager.publishOnTopic(GraphName.of("/initialpose"), PoseWithCovarianceStamped.class);
			goalPublishHandler = TopicManager.publishOnTopic(GraphName.of("/move_base_simple/goal"), PoseStamped.class);
			mapMetaDataSubscribeHandler = TopicManager.subscribeToTopic(GraphName.of("/map_metadata"), MapMetaData.class);
			poseSubscribeHandler = TopicManager.subscribeToTopic(GraphName.of("/amcl_pose"), PoseWithCovarianceStamped.class);

			mapMetaDataSubscribeHandler.subscribe(this::handleMapMetaData);
			poseSubscribeHandler.subscribe(this::handlePose);
		}

		/**
		 * Analyze pose estimate request and publish a message to do pose estimate
		 *
		 * @param request pose estimate request
		 */
		private void doPoseEstimate(@NonNull PoseEstimateMsgModel request) {
			if (!poseEstimatePublishHandler.isReady()) {
				logger.warn("Pose estimate publisher not ready. Dropping request.");
				return;
			}

			if (!isMapMetaDataLoaded) {
				logger.warn("Map metadata not ready. Dropping request.");
				return;
			}

			PoseWithCovarianceStamped msg = poseEstimatePublishHandler.newMessage();
			msg.getHeader().setStamp(NodeManager.getCurrentTime());
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

			poseEstimatePublishHandler.publish(msg);
		}

		/**
		 * Analyze navigation goal request and publish a message to specify navigation goal.
		 *
		 * @param request navigation goal request
		 */
		private void doNavigationGoal(@NonNull NavigationGoalMsgModel request) {
			if (!goalPublishHandler.isReady()) {
				logger.warn("Goal publisher not ready. Dropping request.");
				return;
			}

			if (!isMapMetaDataLoaded) {
				logger.warn("Map metadata not ready. Dropping request.");
				return;
			}

			PoseStamped msg = goalPublishHandler.newMessage();
			msg.getHeader().setStamp(NodeManager.getCurrentTime());
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

			goalPublishHandler.publish(msg);
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
				logger.info(
						String.format(
								"Map metadata in position: w%d h%d x%f y%f r%f",
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
			Quaternion orientation = message.getPose().getPose().getOrientation();

			// TODO: broadcast pose to client
		}

	}
}
