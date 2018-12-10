package com.ericwen229.node;

import geometry_msgs.Twist;
import org.ros.exception.RosRuntimeException;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

public class RoverControllerNode implements NodeMain {

    // ========== constants ==========

    private static final double frequency = 10.0;
    private static final long loopInterval = hzToMillisecondInterval(frequency);

    // ========== members ==========

    private Publisher<Twist> publisher = null;

    // ========== interface required methods ==========

    public GraphName getDefaultNodeName() {
        return GraphName.of("controller");
    }

    public void onStart(ConnectedNode connectedNode) {
        // TODO: set topic name via config or central management
        publisher = connectedNode.newPublisher("/cmd_vel", Twist._TYPE);
    }

    public void onShutdown(Node node) {
    }

    public void onShutdownComplete(Node node) {
    }

    public void onError(Node node, Throwable throwable) {
    }

    // ========== methods ==========

    public void publish(double linear, double angular) {
        if (publisher == null)
            throw new RosRuntimeException("");

        Twist msg = publisher.newMessage();
        msg.getLinear().setX(linear);
        msg.getAngular().setZ(angular);
        publisher.publish(msg);
    }

    // ========== utils ==========

    public static long hzToMillisecondInterval(double hz) {
        return (long)(1000 / hz);
    }

}
