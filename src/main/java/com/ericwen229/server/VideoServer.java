package com.ericwen229.server;

import com.ericwen229.topic.TopicManager;
import com.ericwen229.util.Image;
import lombok.NonNull;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.ros.namespace.GraphName;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.logging.Logger;

public class VideoServer extends WebSocketServer {

	private static final Logger logger = Logger.getLogger(VideoServer.class.getName());

	// ========== constructor ==========

	public VideoServer(@NonNull InetSocketAddress address) {
		super(address);
		TopicManager.subscribeToTopic(GraphName.of("/camera/rgb/image_raw"), sensor_msgs.Image._TYPE, this::imageMessageHandler);
	}

	// ========== overridden methods ==========

	public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
		logger.warning(String.format("Video server connection established: %s", webSocket.getRemoteSocketAddress()));
	}

	public void onClose(WebSocket webSocket, int i, String s, boolean b) {
		logger.info(String.format("Video server connection closed: %s", webSocket.getRemoteSocketAddress()));
	}

	public void onMessage(WebSocket webSocket, String s) {
		logger.warning(String.format("Unexpected message to video server from: %s", webSocket.getRemoteSocketAddress()));
	}

	public void onError(WebSocket webSocket, Exception e) {
		logger.warning(String.format("Video server exception: %s", e.getClass().getName()));
		webSocket.close();
	}

	public void onStart() {
		logger.info(String.format("Video server URI: %s", getAddress()));
	}

	// ========== main logic ==========

	public void imageMessageHandler(sensor_msgs.Image imageMsg) {
		BufferedImage image = Image.imageFromMessage(imageMsg);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			ImageIO.write(image, "jpeg", outputStream);
		} catch (IOException e) {
			// TODO
			throw new RuntimeException();
		}
		broadcast(Base64.getEncoder().encodeToString(outputStream.toByteArray()));
	}

}
