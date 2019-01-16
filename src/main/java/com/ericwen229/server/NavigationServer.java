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

public class NavigationServer extends WebSocketServer {

	private static final Gson gson;
	private static final Log logger = LogFactory.getLog(NavigationServer.class);

	private final NavigationManager navigationManager;

	static {
		RuntimeTypeAdapterFactory<RequestMsgModel> requestRuntimeTypeAdapterFactory
				= RuntimeTypeAdapterFactory
				.of(RequestMsgModel.class, RequestMsgModel.typeFieldName)
				.registerSubtype(PoseEstimateMsgModel.class, PoseEstimateMsgModel.typeFieldValue)
				.registerSubtype(NavigationGoalMsgModel.class, NavigationGoalMsgModel.typeFieldValue);
		gson = new GsonBuilder()
				.registerTypeAdapterFactory(requestRuntimeTypeAdapterFactory)
				.create();
	}

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
//		if (request.type == PoseEstimateMsgModel.typeFieldValue) {
//			navigationManager.doPoseEstimate((PoseEstimateMsgModel)request);
//		}
//		else if (request.type == NavigationGoalMsgModel.typeFieldValue) {
//			navigationManager.doNavigationGoal((NavigationGoalMsgModel)request);
//		}
//		else {
//			logger.warn("Unknown request type: " + request.type);
//		}
		navigationManager.doPoseEstimate((PoseEstimateMsgModel)request);
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


	private class NavigationManager {

		private final TopicPublishHandler<PoseWithCovarianceStamped> poseEstimatePublishHandler;
		private final TopicPublishHandler<PoseStamped> goalPublishHandler;
		private final TopicSubscribeHandler<MapMetaData> mapMetaDataSubscribeHandler;
		private final TopicSubscribeHandler<PoseWithCovarianceStamped> poseSubscribeHandler;

		private final Object mapMetaDataMutex = new Object();
		private volatile boolean isMapMetaDataLoaded = false;
		private volatile int mapWidth;
		private volatile int mapHeight;
		private volatile double originX;
		private volatile double originY;
		private volatile double resolution;


		private NavigationManager() {
			poseEstimatePublishHandler = TopicManager.publishOnTopic(GraphName.of("/initialpose"), PoseWithCovarianceStamped.class);
			goalPublishHandler = TopicManager.publishOnTopic(GraphName.of("/move_base_simple/goal"), PoseStamped.class);
			mapMetaDataSubscribeHandler = TopicManager.subscribeToTopic(GraphName.of("/map_metadata"), MapMetaData.class);
			poseSubscribeHandler = TopicManager.subscribeToTopic(GraphName.of("/amcl_pose"), PoseWithCovarianceStamped.class);

			mapMetaDataSubscribeHandler.subscribe(this::handleMapMetaData);
			poseSubscribeHandler.subscribe(this::handlePose);
		}

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

		private void doNavigationGoal(@NonNull NavigationGoalMsgModel request) {
		}

		private void handleMapMetaData(@NonNull MapMetaData message) {
			synchronized (mapMetaDataMutex) {
				isMapMetaDataLoaded = true;
				mapWidth = message.getWidth();
				mapHeight = message.getHeight();
				originX = message.getOrigin().getPosition().getX();
				originY = message.getOrigin().getPosition().getY();
				resolution = message.getResolution();
			}
		}

		private void handlePose(@NonNull PoseWithCovarianceStamped message) {
			Point position = message.getPose().getPose().getPosition();
			Quaternion orientation = message.getPose().getPose().getOrientation();
		}

	}
}
