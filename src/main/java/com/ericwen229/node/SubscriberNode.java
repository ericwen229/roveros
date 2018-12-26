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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

class SubscriberNode<T extends Message> implements NodeMain {

	@Getter private final GraphName topicName;
	@Getter private final Class<T> topicType;

	private final Set<Consumer<T>> consumers;

	SubscriberNode(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		this.topicName = topicName;
		this.topicType = topicType;
		consumers = new HashSet<>();
	}

	@Override
	public GraphName getDefaultNodeName() {
		return Name.getSubscriberNodeName(topicName);
	}

	@Override
	public void onStart(ConnectedNode connectedNode) {
		Logger.getGlobal().info(
				String.format(
						"Subscriber node %s at %s starting",
						getDefaultNodeName(),
						connectedNode.getUri()));
		Subscriber<T> subscriber = connectedNode.newSubscriber(topicName, TopicManager.topicTypeObjectToTopicTypeStr(topicType));
		subscriber.addMessageListener(this::accept);
	}

	@Override
	public void onShutdown(Node node) {
		Logger.getGlobal().info(
				String.format(
						"Subscriber node %s at %s shutting down",
						getDefaultNodeName(),
						node.getUri()));
	}

	@Override
	public void onShutdownComplete(Node node) {}

	@Override
	public void onError(Node node, Throwable throwable) {
		Logger.getGlobal().severe(
				String.format(
						"Subscriber node %s at %s error: %s",
						getDefaultNodeName(),
						node.getUri(),
						throwable));
	}

	SubscriberNodeHandler<T> createHandler() {
		return new SubscriberNodeHandler<>(this);
	}

	private void accept(@NonNull T message) {
		synchronized (consumers) {
			for (Consumer<T> consumer: consumers) {
				consumer.accept(message);
			}
		}
	}

	void subscribe(@NonNull Consumer<T> consumer) {
		synchronized (consumers) {
			consumers.add(consumer);
		}
	}

	void unsubscribe(@NonNull Consumer<T> consumer) {
		synchronized (consumers) {
			consumers.remove(consumer);
		}
	}

}
