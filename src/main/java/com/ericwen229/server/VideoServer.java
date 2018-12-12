package com.ericwen229.server;

import com.ericwen229.node.NodeManager;
import com.ericwen229.node.RoverVideoMonitorNode;
import com.ericwen229.util.ImageUtil;
import com.ericwen229.util.pattern.Observer;
import lombok.NonNull;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import sensor_msgs.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Base64;

public class VideoServer extends WebSocketServer implements Observer<Image> {

    // ========== constructor ==========

    public VideoServer(@NonNull InetSocketAddress address) {
        super(address);
        System.out.println(String.format("[video server starting on %s:%d]", address.getHostName(), address.getPort()));

        NodeManager.acquireVideoMonitorNode().addObserver(this);
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
        // TODO: server startup
        System.out.println("[video server is up]");
    }

    // ========== interface required methods ==========

    public void notify(Image imageMsg) {
        BufferedImage image = ImageUtil.imageFromMessage(imageMsg);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpeg", outputStream);
        }
        catch (IOException e) {
            // TODO
            throw new RuntimeException();
        }
        broadcast(Base64.getEncoder().encodeToString(outputStream.toByteArray()));
    }

}
