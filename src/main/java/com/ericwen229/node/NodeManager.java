package com.ericwen229.node;

import com.ericwen229.util.Config;
import lombok.NonNull;
import org.ros.internal.message.Message;
import org.ros.namespace.GraphName;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;

import java.net.URI;
import java.util.HashMap;

public class NodeManager {

	private static NodeConfiguration nodeConfig = null;
	private static final NodeMainExecutor nodeExecutor = DefaultNodeMainExecutor.newDefault();
	private static final HashMap<GraphName, PublisherNode> topicNameToPublisherNode = new HashMap<>();
	private static final HashMap<GraphName, SubscriberNode> topicNameToSubscriberNode = new HashMap<>();

	private static NodeConfiguration acquireNodeConfiguration() {
		if (nodeConfig == null) {
			String host = Config.getPropertyAsString("host");
			String masterURI = Config.getPropertyAsString("masterURI");
			nodeConfig = NodeConfiguration.newPublic(host, URI.create(masterURI));
		}
		return nodeConfig;
	}

	public static <T extends Message> PublisherNodeHandler<T> acquirePublisherNodeHandler(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		synchronized (topicNameToPublisherNode) {
			PublisherNode<T> publisherNode = retrievePublisherNodeWithTopicNameAndType(topicName, topicType);
			return publisherNode.createHandler();
		}
	}

	public static <T extends Message> SubscriberNodeHandler<T> acquireSubscriberNodeHandler(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		synchronized (topicNameToSubscriberNode) {
			SubscriberNode<T> subscriberNode = retrieveSubscriberNodeWithTopicNameAndType(topicName, topicType);
			return subscriberNode.createHandler();
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends Message> PublisherNode<T> retrievePublisherNodeWithTopicNameAndType(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		synchronized (topicNameToPublisherNode) {
			// same name and same type: create a new handler and return handler
			// otherwise: error
			PublisherNode node = topicNameToPublisherNode.getOrDefault(topicName, null);
			if (node == null) {
				// CASE 1: no same name - create
				PublisherNode<T> newNode = new PublisherNode<>(topicName, topicType);
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
	}

	@SuppressWarnings("unchecked")
	private static <T extends Message> SubscriberNode<T> retrieveSubscriberNodeWithTopicNameAndType(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		synchronized (topicNameToSubscriberNode) {
			// same name and same type: create a new handler and return handler
			// otherwise: error
			SubscriberNode node = topicNameToSubscriberNode.getOrDefault(topicName, null);
			if (node == null) {
				// CASE 1: no same name - create
				SubscriberNode<T> newNode = new SubscriberNode<>(topicName, topicType);
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
	}

	private static void executeNode(@NonNull NodeMain node) {
		nodeExecutor.execute(node, acquireNodeConfiguration());
	}

	static void shutdownPublisherNode(@NonNull PublisherNode<? extends Message> node) {
		topicNameToPublisherNode.remove(node.getTopicName());
		nodeExecutor.shutdownNodeMain(node);
	}

	static void shutdownSubscriberNode(@NonNull SubscriberNode<? extends Message> node) {
		topicNameToSubscriberNode.remove(node.getTopicName());
		nodeExecutor.shutdownNodeMain(node);
	}

}
