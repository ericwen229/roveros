package com.ericwen229.server;

import com.ericwen229.node.NodeManager;
import com.ericwen229.node.RoverControllerNode;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.ros.exception.RosRuntimeException;

import java.net.InetSocketAddress;
import java.util.Scanner;

public class ControlServer extends WebSocketServer {

    private final RoverControllerNode controllerNode;

    public ControlServer(InetSocketAddress address) {
        super(address);
        System.out.println(String.format("[server starting on %s:%d]", address.getHostName(), address.getPort()));

        controllerNode = NodeManager.acquireRoverControllerNode();
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        // TODO: client connection handling
        System.out.println("[a client just came]");
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        // TODO: client removal handling
        System.out.println("[a client has left]");
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        // TODO: protocal design
        Scanner scanner = new Scanner(s);
        double linear = scanner.nextDouble();
        double angular = scanner.nextDouble();

        try {
            // TODO: publish rate should be independent of message receive rate
            controllerNode.publish(linear, angular);
        }
        catch (RosRuntimeException e) {
            System.out.println("[publish failed]");
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        // TODO: error handling
        System.out.println(String.format("[exception: %s]", e.toString()));
    }

    @Override
    public void onStart() {
        // TODO: server startup
        System.out.println("[server is up]");
    }
    
}
