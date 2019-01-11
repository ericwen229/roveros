package com.ericwen229.server;

import com.ericwen229.node.TopicSubscribeHandler;
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

	public VideoServer(@NonNull InetSocketAddress address) {
		super(address);
		TopicSubscribeHandler<sensor_msgs.Image> handler =
				TopicManager.subscribeToTopic(
						GraphName.of("/camera/rgb/image_color"),
						sensor_msgs.Image.class);
		handler.subscribe(this::imageMessageHandler);
	}

	public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
		Logger.getGlobal().warning(
				String.format("Video server connection established: %s", webSocket.getRemoteSocketAddress()));
	}

	public void onClose(WebSocket webSocket, int i, String s, boolean b) {
		Logger.getGlobal().info(
				String.format("Video server connection closed: %s", webSocket.getRemoteSocketAddress()));
	}

	public void onMessage(WebSocket webSocket, String s) {
		Logger.getGlobal().warning(
				String.format("Unexpected message to video server from: %s", webSocket.getRemoteSocketAddress()));
	}

	public void onError(WebSocket webSocket, Exception e) {
		Logger.getGlobal().warning(
				String.format("Video server exception: %s", e.getClass().getName()));
		webSocket.close();
	}

	public void onStart() {
		Logger.getGlobal().info(
				String.format("Video server URI: %s", getAddress()));
	}

	public void imageMessageHandler(sensor_msgs.Image imageMsg) {
		BufferedImage image = Image.imageFromMessage(imageMsg);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			ImageIO.write(image, "jpeg", outputStream);
		} catch (IOException e) {
			Logger.getGlobal().severe("Image IO error");
		}
		broadcast(Base64.getEncoder().encodeToString(outputStream.toByteArray()));
	}

}
