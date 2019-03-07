package com.ericwen229.node;

import lombok.NonNull;
import org.ros.internal.message.Message;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.*;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.net.URI;
import java.util.logging.Logger;

/**
 * This class implements an ROS node. Most of its functionality is quite basic, except that
 * it provides methods of creating publishers/subscribers.
 */
public class RoverOSNode implements NodeMain {

    /**
     * Name of the ROS node.
     */
    private final GraphName nodeName;

    /**
     * Configuration of the ROS node (host address, URI of master, accessibility, etc.).
     */
    private final NodeConfiguration nodeConfig;

    /**
     * Executor of the ROS node (shared among all instances).
     */
    private static final NodeMainExecutor nodeExecutor = DefaultNodeMainExecutor.newDefault();

    /**
     * Node that has connected to master (as the factory of publishers and subscribers).
     */
    private ConnectedNode connectedNode = null;

    /**
     * Mutex of connected node.
     */
    private final Object connectedNodeMutex = new Object();

    /**
     * Create an ROS node that is publicly accessible.
     *
     * @param nodeName name of node
     * @param host host address of node
     * @param masterURI URI of master
     * @return newly created public node
     */
    public static RoverOSNode newPublicNode(GraphName nodeName, @NonNull String host, @NonNull URI masterURI) {
        return new RoverOSNode(nodeName, host, masterURI, false);
    }

    /**
     * Create an ROS node that is only locally accessible.
     *
     * @param nodeName name of node
     * @param masterURI URI of master
     * @return newly created private node
     */
    public static RoverOSNode newPrivateNode(@NonNull GraphName nodeName, @NonNull URI masterURI) {
        return new RoverOSNode(nodeName, null, masterURI, true);
    }

    /**
     * Construct an ROS node (public or private).
     *
     * @param nodeName name of node
     * @param host host address of node
     * @param masterURI URI of master
     * @param isLocalhostOnly true if the node is only locally accessible
     */
    private RoverOSNode(@NonNull GraphName nodeName, String host, URI masterURI, boolean isLocalhostOnly) {
        this.nodeName = nodeName;
        if (!isLocalhostOnly) {
            this.nodeConfig = NodeConfiguration.newPublic(host, masterURI);
        }
        else {
            this.nodeConfig = NodeConfiguration.newPrivate(masterURI);
        }
    }

    /**
     * Run the ROS node.
     */
    public void run() {
        nodeExecutor.execute(this, nodeConfig);
    }

    /**
     * Check whether the ROS node has successfully registered at the master.
     *
     * @return true if node has successfully registered at the master
     */
    public boolean ready() {
        synchronized (connectedNodeMutex) {
            return this.connectedNode != null;
        }
    }

    /**
     * Create a publisher on topic.
     *
     * @param topicName name of topic
     * @param topicTypeObject type object of topic
     * @param <T> type of topic
     * @return newly created publisher
     */
    public <T extends Message> Publisher<T>
    publishOnTopic(@NonNull GraphName topicName, @NonNull Class<T> topicTypeObject) {
        if (!ready()) {
            throw new RuntimeException("RoverOSNode not ready yet");
        }
        synchronized (connectedNodeMutex) {
            return connectedNode.newPublisher(topicName, topicTypeObjectToTopicTypeStr(topicTypeObject));
        }
    }

    /**
     * Create a subscriber on topic.
     *
     * @param topicName name of topic
     * @param topicTypeObject type object of topic
     * @param <T> type of topic
     * @return newly created subscriber
     */
    public <T extends Message> Subscriber<T>
    subscribeToTopic(@NonNull GraphName topicName, @NonNull Class<T> topicTypeObject) {
        if (!ready()) {
            throw new RuntimeException("RoverOSNode not ready yet");
        }
        synchronized (connectedNodeMutex) {
            return connectedNode.newSubscriber(topicName, topicTypeObjectToTopicTypeStr(topicTypeObject));
        }
    }

    // ========================
    // node lifecycle callbacks
    // ========================

    @Override
    public GraphName getDefaultNodeName() {
        return nodeName;
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Logger.getGlobal().info(
                String.format("RoverOS node %s starting", nodeName));
        synchronized (connectedNodeMutex) {
            this.connectedNode = connectedNode;
        }
    }

    @Override
    public void onShutdown(Node node) {
        Logger.getGlobal().info(
                String.format("RoverOS node %s shutting down", nodeName));
        synchronized (connectedNodeMutex) {
            this.connectedNode = null;
        }
    }

    @Override
    public void onShutdownComplete(Node node) {
        Logger.getGlobal().info(
                String.format("RoverOS node %s shut down complete", nodeName));
    }

    @Override
    public void onError(Node node, Throwable throwable) {
        Logger.getGlobal().severe(
                String.format("RoverOS node %s error: %s", nodeName, throwable.getClass()));
        synchronized (connectedNodeMutex) {
            this.connectedNode = null;
        }
    }

    // =====
    // utils
    // =====

    /**
     * Retrieve value of the static field _TYPE using reflection.
     *
     * @param topicTypeObject type object of topic
     * @return value of the static field _TYPE
     */
    private String topicTypeObjectToTopicTypeStr(@NonNull Class topicTypeObject) {
        try {
            return (String)topicTypeObject.getField("_TYPE").get(null);
        }
        catch (NoSuchFieldException e) {
            throw new RuntimeException(
                    String.format("Static field \"_TYPE\" missing in topic type %s", topicTypeObject));
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(
                    String.format("Cannot access field \"_TYPE\" in topic type %s", topicTypeObject));
        }
    }

    /**
     * Get current ROS time. The time is normally used to stamp outgoing messages.
     *
     * @return current ROS time.
     */
    public Time getCurrentTime() {
        return nodeConfig.getTimeProvider().getCurrentTime();
    }
}
