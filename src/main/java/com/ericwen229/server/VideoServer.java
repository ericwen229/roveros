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
		// TODO: client connection handling
		System.out.println("[a client just came to video server]");
	}

	public void onClose(WebSocket webSocket, int i, String s, boolean b) {
		// TODO: client removal handling
		System.out.println("[a client has left video server]");
	}

	public void onMessage(WebSocket webSocket, String s) {
		// TODO: ignore message
	}

	public void onError(WebSocket webSocket, Exception e) {
		// TODO: error handling
		System.out.println(String.format("[video server exception: %s]", e.toString()));
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
