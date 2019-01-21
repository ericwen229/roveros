package com.ericwen229.node;

import com.ericwen229.util.Config;
import lombok.NonNull;
import org.ros.internal.message.Message;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;

import java.net.URI;
import java.util.HashMap;

/**
 * This class manages ROS nodes. Each node is used for either message
 * publishing or subscribing. Given the topic to publish on or subscribe
 * to, a node will be created if no such node has been created and a handler
 * of it will be returned. If there already exists a node corresponding to
 * that topic, only a new handler is created.
 *
 * <p>Handlers can be used to publish on or to subscribe to topics. They can
 * also be closed when not needed anymore. When the last active handler is
 * returned, the node will automatically be shutdown.
 *
 * <p>This class is thread safe.
 *
 * @see TopicPublishHandler
 * @see TopicSubscribeHandler
 */
public class NodeManager {

	/**
	 * Mutex on node configuration.
	 */
	private static final Object nodeConfigurationMutex = new Object();

	/**
	 * Node configuration object shared by nodes. It is created at its first
	 * use. It contains host address and master URI specified by {@link Config}.
	 */
	private static NodeConfiguration nodeConfig = null;

	/**
	 * Default node executor. This class uses it to execute or shut down nodes.
	 */
	private static final NodeMainExecutor nodeExecutor = DefaultNodeMainExecutor.newDefault();

	/**
	 * Publisher nodes indexed by topic name.
	 */
	private static final HashMap<GraphName, PublisherNode> topicNameToPublisherNode = new HashMap<>();

	/**
	 * Subscriber nodes indexed by topic name.
	 */
	private static final HashMap<GraphName, SubscriberNode> topicNameToSubscriberNode = new HashMap<>();

	/**
	 * Specify the host address and master URI used by ROS nodes.
	 *
	 * @param host host address used by ROS nodes so that other nodes can use it to reach them
	 * @param masterURI URI of master node
	 */
	public static void config(@NonNull String host, @NonNull String masterURI) {
		synchronized (nodeConfigurationMutex) {
			if (nodeConfig != null) {
				throw new RuntimeException("Node configuration already exists");
			}
			nodeConfig = NodeConfiguration.newPublic(host, URI.create(masterURI));
		}
	}

	/**
	 * Acquire node configuration shared between all nodes. Create one if not created yet.
	 * Configuration contains host address and master URI specified by {@link Config}.
	 *
	 * @return node configuration
	 */
	private static NodeConfiguration acquireNodeConfiguration() {
		synchronized (nodeConfigurationMutex) {
			if (nodeConfig == null) {
				throw new RuntimeException("Node configuration missing");
			}
			return nodeConfig;
		}
	}

	/**
	 * Acquire topic publish handler.
	 *
	 * <p>If there's not such a node yet, create one then create a handler of it.
	 * Otherwise just create a handler of existing node. The nodes are indexed by
	 * topic name. Nodes with the same topic name are supposed to have the same
	 * topic type, or an exception will be thrown.
	 *
	 * @param topicName name of topic to publish on
	 * @param topicType class object of topic type
	 * @param <T> type of topic to publish on
	 * @return topic publish handler
	 */
	public static <T extends Message> TopicPublishHandler<T> acquireTopicPublishHandler(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		synchronized (topicNameToPublisherNode) {
			PublisherNode<T> publisherNode = acquirePublisherNode(topicName, topicType);
			return publisherNode.createHandler();
		}
	}

	/**
	 * Return (give back) topic publish handler.
	 *
	 * <p>If returned handler is the last handler of node, then node is shut down and removed.
	 *
	 * @param handler handler to return
	 */
	static void returnTopicPublishHandler(@NonNull TopicPublishHandler handler) {
		synchronized (topicNameToPublisherNode) {
			PublisherNode node = handler.getPublisherNode();
			node.returnHandler(handler);
			if (node.getHandlerCount() == 0) {
				topicNameToPublisherNode.remove(node.getTopicName());
				shutdownNode(node);
			}
		}
	}

	/**
	 * Acquire topic subscribe handler.
	 *
	 * <p>If there's not such a node yet, create one then create a handler of it.
	 * Otherwise just create a handler of existing node. The nodes are indexed by
	 * topic name. Nodes with the same topic name are supposed to have the same
	 * topic type, or an exception will be thrown.
	 *
	 * @param topicName name of topic to subscribe to
	 * @param topicType class object of topic type
	 * @param <T> type of topic to publish on
	 * @return topic subscribe handler
	 */
	public static <T extends Message> TopicSubscribeHandler<T> acquireTopicSubscribeHandler(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		synchronized (topicNameToSubscriberNode) {
			SubscriberNode<T> subscriberNode = acquireSubscriberNode(topicName, topicType);
			return subscriberNode.createHandler();
		}
	}

	/**
	 * Return (give back) topic subscribe handler.
	 *
	 * <p>If returned handler is the last handler of node, then node is shut down and removed.
	 *
	 * @param handler handler to return
	 */
	static void returnTopicSubscribeHandler(@NonNull TopicSubscribeHandler handler) {
		synchronized (topicNameToSubscriberNode) {
			SubscriberNode node = handler.getSubscriberNode();
			node.returnHandler(handler);
			if (node.getHandlerCount() == 0) {
				topicNameToSubscriberNode.remove(node.getTopicName());
				shutdownNode(node);
			}
		}
	}

	/**
	 * Acquire publisher node.
	 *
	 * <p>If there's not such a node yet, create one. Otherwise return the existing
	 * node indexed by topic name. Nodes with the same topic name are supposed to
	 * have the same topic type, or an exception will be thrown.
	 *
	 * @param topicName name of topic on which the node is publishing
	 * @param topicType class object of topic type
	 * @param <T> type of topic on which the node is publishing
	 * @return publisher node
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Message> PublisherNode<T> acquirePublisherNode(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		// same name and same type: create a new handler and return handler
		// otherwise: error
		PublisherNode node = topicNameToPublisherNode.getOrDefault(topicName, null);
		if (node == null) {
			// CASE 1: no same name - create
			PublisherNode<T> newNode = new PublisherNode<>(topicName, topicType);
			topicNameToPublisherNode.put(topicName, newNode);
			executeNode(newNode);
			return newNode;
		} else if (node.getTopicType().equals(topicType)) {
			// CASE 2: same name and same type - return
			return node;
		} else {
			throw new RuntimeException(
					String.format(
							"Type conflict on topic %s: %s - %s",
							topicName,
							node.getTopicType(),
							topicType));
		}
	}

	/**
	 * Acquire subscriber node.
	 *
	 * <p>If there's not such a node yet, create one. Otherwise return the existing
	 * node indexed by topic name. Nodes with the same topic name are supposed to
	 * have the same topic type, or an exception will be thrown.
	 *
	 * @param topicName name of topic to which the node is subscribing
	 * @param topicType class object of topic type
	 * @param <T> type of topic to which the node is subscribing
	 * @return subscriber node
	 */
	@SuppressWarnings("unchecked")
	private static <T extends Message> SubscriberNode<T> acquireSubscriberNode(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		// same name and same type: create a new handler and return handler
		// otherwise: error
		SubscriberNode node = topicNameToSubscriberNode.getOrDefault(topicName, null);
		if (node == null) {
			// CASE 1: no same name - create
			SubscriberNode<T> newNode = new SubscriberNode<>(topicName, topicType);
			topicNameToSubscriberNode.put(topicName, newNode);
			executeNode(newNode);
			return newNode;
		} else if (node.getTopicType().equals(topicType)) {
			// CASE 2: same name and same type - return
			return node;
		} else {
			throw new RuntimeException(
					String.format(
							"Type conflict on topic %s: %s - %s",
							topicName,
							node.getTopicType(),
							topicType));
		}
	}

	/**
	 * Execute given node with shared configuration.
	 *
	 * @param node node to execute
	 */
	private static void executeNode(@NonNull NodeMain node) {
		nodeExecutor.execute(node, acquireNodeConfiguration());
	}

	/**
	 * Shut down given node.
	 *
	 * @param node node to shut down
	 */
	private static void shutdownNode(@NonNull NodeMain node) {
		nodeExecutor.shutdownNodeMain(node);
	}

	/**
	 * Get current ROS time (either system time or simulated time).
	 *
	 * @return ROS time
	 */
	public static Time getCurrentTime() {
		return acquireNodeConfiguration().getTimeProvider().getCurrentTime();
	}

}
