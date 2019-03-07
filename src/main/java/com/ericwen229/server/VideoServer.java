package com.ericwen229.server;

import com.ericwen229.node.RoverOSNode;
import com.ericwen229.server.message.response.ImageMsgModel;
import com.ericwen229.util.Image;
import com.google.gson.Gson;
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
	 * Gson object used for message serialize and deserialize.
	 */
	private static final Gson gson = new Gson();

	/**
	 * Construct server with given address.
	 *
	 * @param address address to which server will listen
	 */
	public VideoServer(@NonNull RoverOSNode node, @NonNull InetSocketAddress address) {
		super(address);
		Subscriber<sensor_msgs.Image> handler =
				node.subscribeToTopic(
						GraphName.of("/camera/rgb/image_color"),
						sensor_msgs.Image.class);
		handler.addMessageListener(this::imageMessageHandler);
	}

	@Override
	public void onStart() {
		Logger.getGlobal().info(
				String.format("RoverOS video server starting at %s", getAddress()));
	}

	@Override
	public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
		Logger.getGlobal().info(
				String.format("RoverOS video server established connection to %s", webSocket.getRemoteSocketAddress()));
	}

	@Override
	public void onClose(WebSocket webSocket, int i, String s, boolean b) {
		Logger.getGlobal().info(
				String.format("RoverOS video server closing connection to %s", webSocket.getRemoteSocketAddress()));
	}

	@Override
	public void onMessage(WebSocket webSocket, String s) {
		Logger.getGlobal().warning(
				String.format("Unexpected message to video server from: %s", webSocket.getRemoteSocketAddress()));
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
	 * Callback invoked when image message received.
	 *
	 * @param imageMsg received image message
	 */
	private void imageMessageHandler(sensor_msgs.Image imageMsg) {
		BufferedImage image = Image.imageMessageToBufferdImage(imageMsg);
		byte[] imageBytes = Image.bufferedImageToByteArray(image, "jpeg");
		String base64EncodedImageStr = Base64.getEncoder().encodeToString(imageBytes);

		ImageMsgModel msg = new ImageMsgModel();
		msg.base64EncodedImageStr = base64EncodedImageStr;
		broadcast(gson.toJson(msg));
	}

}
