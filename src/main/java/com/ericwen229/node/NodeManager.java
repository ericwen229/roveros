package com.ericwen229.node;

import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

public class NodeManager {

    // TODO: more configuration
    private static final NodeMainExecutor defaultNodeExecutor = DefaultNodeMainExecutor.newDefault();
    private static final NodeConfiguration defaultNodeConfig = NodeConfiguration.newPrivate();

    private static RoverControllerNode controllerNode = null;
    private static RoverVideoMonitorNode videoMonitorNode = null;

    public static RoverControllerNode acquireControllerNode() {
        // TODO: may not be singleton in the future
        if (controllerNode == null) {
            controllerNode = new RoverControllerNode();
            defaultNodeExecutor.execute(controllerNode, defaultNodeConfig);
        }
        return controllerNode;
    }

    public static RoverVideoMonitorNode acquireVideoMonitorNode() {
        if (videoMonitorNode == null) {
            videoMonitorNode = new RoverVideoMonitorNode();
            defaultNodeExecutor.execute(videoMonitorNode, defaultNodeConfig);
        }
        return videoMonitorNode;
    }

}
