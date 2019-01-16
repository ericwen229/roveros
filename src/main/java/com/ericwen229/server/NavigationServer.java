package com.ericwen229.server;

import com.ericwen229.node.TopicPublishHandler;
import com.ericwen229.node.TopicSubscribeHandler;
import com.ericwen229.server.model.NavigationGoalMsgModel;
import com.ericwen229.server.model.PoseEstimateMsgModel;
import com.ericwen229.server.model.RequestMsgModel;
import com.ericwen229.topic.TopicManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import lombok.NonNull;
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

		private final TopicPublishHandler<geometry_msgs.PoseWithCovarianceStamped> poseEstimatePublishHandler;
		private final TopicPublishHandler<geometry_msgs.PoseStamped> goalPublishHandler;
		private final TopicSubscribeHandler<nav_msgs.MapMetaData> mapMetaDataSubscribeHandler;
		private final TopicSubscribeHandler<geometry_msgs.PoseWithCovarianceStamped> poseSubscribeHandler;

		private NavigationManager() {
			poseEstimatePublishHandler = TopicManager.publishOnTopic(GraphName.of("/initialpose"), geometry_msgs.PoseWithCovarianceStamped.class);
			goalPublishHandler = TopicManager.publishOnTopic(GraphName.of("/move_base_simple/goal"), geometry_msgs.PoseStamped.class);
			mapMetaDataSubscribeHandler = TopicManager.subscribeToTopic(GraphName.of("/map_metadata"), nav_msgs.MapMetaData.class);
			poseSubscribeHandler = TopicManager.subscribeToTopic(GraphName.of("/amcl_pose"), geometry_msgs.PoseWithCovarianceStamped.class);
		}

	}
}
