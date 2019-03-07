package com.ericwen229.server;

import com.ericwen229.node.RoverOSNode;
import com.ericwen229.util.Image;
import lombok.NonNull;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.ros.namespace.GraphName;
import org.ros.node.topic.Subscriber;

import java.awt.image.BufferedImage;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * This class implements a websocket server used to broadcast
 * images from Turtlebot camera.
 *
 * <p>The server broadcasts base64 encoded jpeg images.
 */
public class VideoServer extends WebSocketServer {

	/**
	 * Construct server with given address.
	 *
	 * @param address address to which server will listen
	 */
	public VideoServer(@NonNull RoverOSNode node, @NonNull InetSocketAddress address) {
		super(address);
		while (!node.ready()) {}
		Subscriber<sensor_msgs.Image> handler =
				node.subscribeToTopic(
						GraphName.of("/camera/rgb/image_color"),
						sensor_msgs.Image.class);
		handler.addMessageListener(this::imageMessageHandler);
	}

	@Override
	public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
		Logger.getGlobal().warning(
				String.format("Video server connection established: %s", webSocket.getRemoteSocketAddress()));
	}

	@Override
	public void onClose(WebSocket webSocket, int i, String s, boolean b) {
		Logger.getGlobal().info(
				String.format("Video server connection closed: %s", webSocket.getRemoteSocketAddress()));
	}

	@Override
	public void onMessage(WebSocket webSocket, String s) {
		Logger.getGlobal().warning(
				String.format("Unexpected message to video server from: %s", webSocket.getRemoteSocketAddress()));
	}

	@Override
	public void onError(WebSocket webSocket, Exception e) {
		Logger.getGlobal().warning(
				String.format("Video server exception: %s", e.getClass().getName()));
		webSocket.close();
	}

	@Override
	public void onStart() {
		Logger.getGlobal().info(
				String.format("Video server URI: %s", getAddress()));
	}

	/**
	 * Callback invoked when image message received.
	 *
	 * @param imageMsg received image message
	 */
	private void imageMessageHandler(sensor_msgs.Image imageMsg) {
		BufferedImage image = Image.imageMessageToBufferdImage(imageMsg);
		byte[] imageBytes = Image.bufferedImageToByteArray(image, "jpeg");
		broadcast(Base64.getEncoder().encodeToString(imageBytes));
	}

}
