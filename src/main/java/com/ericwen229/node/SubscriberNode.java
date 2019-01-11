package com.ericwen229.node;

import com.ericwen229.topic.TopicManager;
import com.ericwen229.util.Name;
import lombok.Getter;
import lombok.NonNull;
import org.ros.internal.message.Message;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class implements ROS nodes that are responsible for subscribing
 * to topics. Each node is associated with one single topic to which
 * node is subscribed.
 *
 * @param <T> type of topic to which the node is subscribed
 *
 * @see NodeManager
 * @see TopicSubscribeHandler
 */
class SubscriberNode<T extends Message> implements NodeMain {

	/**
	 * Name of topic to which current node is subscribing
	 */
	@Getter private final GraphName topicName;

	/**
	 * Type of topic to which current node is subscribing
	 */
	@Getter private final Class<T> topicType;

	/**
	 * Active handlers of current node.
	 */
	private final Set<TopicSubscribeHandler<T>> handlers;

	// ===========
	// constructor
	// ===========

	/**
	 * Create a subscriber node associated with given topic of given type.
	 *
	 * @param topicName name of topic to which the node is subscribing
	 * @param topicType type of topic to which the node is subscribing
	 */
	SubscriberNode(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		this.topicName = topicName;
		this.topicType = topicType;
		handlers = Collections.synchronizedSet(new HashSet<>());
	}

	// ========================
	// node lifecycle callbacks
	// ========================

	/**
	 * Return name of node.
	 *
	 * @return name of node
	 */
	@Override
	public GraphName getDefaultNodeName() {
		return Name.getSubscriberNodeName(topicName);
	}

	/**
	 * Invoked when node has successfully contacted master. Subscriber is created
	 * here.
	 *
	 * @param connectedNode node that's successfully contacted master, used
	 *                      as factory of subscriber
	 */
	@Override
	public void onStart(ConnectedNode connectedNode) {
		connectedNode.getLog().info(
				String.format(
						"Subscriber node %s at %s starting",
						getDefaultNodeName(),
						connectedNode.getUri()));
		Subscriber<T> subscriber = connectedNode.newSubscriber(topicName, TopicManager.topicTypeObjectToTopicTypeStr(topicType));
		subscriber.addMessageListener(this::accept);
	}

	/**
	 * Invoked when node is shutting down.
	 *
	 * @param node node to be shut down
	 */
	@Override
	public void onShutdown(Node node) {
		node.getLog().info(
				String.format(
						"Subscriber node %s at %s shutting down",
						getDefaultNodeName(),
						node.getUri()));
	}

	/**
	 * Invoked when node has been shut down.
	 *
	 * @param node node that's been shut down
	 */
	@Override
	public void onShutdownComplete(Node node) {
		node.getLog().info(
				String.format(
						"Subscriber node %s at %s shut down complete",
						getDefaultNodeName(),
						node.getUri()));
	}

	/**
	 * Invoked when fatal error occurs. After which {@link #onShutdown(Node)} and
	 * {@link #onShutdownComplete(Node)} will be invoked.
	 *
	 * @param node node that raises an error
	 * @param throwable cause of error
	 */
	@Override
	public void onError(Node node, Throwable throwable) {
		node.getLog().fatal(
				String.format(
						"Subscriber node %s at %s error: %s",
						getDefaultNodeName(),
						node.getUri(),
						throwable));
		System.exit(-1);
	}

	// ==================
	// handler management
	// ==================

	/**
	 * Create a subscriber node handler to be used by user program.
	 *
	 * @return subscriber handler created
	 */
	TopicSubscribeHandler<T> createHandler() {
		TopicSubscribeHandler<T> newHandler = new TopicSubscribeHandler<>(this);
		handlers.add(newHandler);
		return newHandler;
	}

	/**
	 * Return (give back) a subscriber handler.
	 *
	 * @param handler handler to return
	 */
	void returnHandler(TopicSubscribeHandler handler) {
		handlers.remove(handler);
	}

	/**
	 * Return number of active handlers.
	 *
	 * @return number of active handlers
	 */
	int getHandlerCount() {
		return handlers.size();
	}

	// =========================
	// message receival callback
	// =========================

	/**
	 * Invoked when there's a new message.
	 *
	 * @param message message received
	 */
	private void accept(@NonNull T message) {
		for (TopicSubscribeHandler<T> handler: handlers) {
			handler.accept(message);
		}
	}

}
