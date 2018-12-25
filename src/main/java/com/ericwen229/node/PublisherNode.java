package com.ericwen229.node;

import com.ericwen229.topic.PublisherHandler;
import com.ericwen229.topic.TopicManager;
import com.ericwen229.util.Name;
import lombok.Getter;
import lombok.NonNull;
import org.ros.internal.message.Message;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import java.util.logging.Logger;

public class PublisherNode<T extends Message> implements NodeMain {

	// ========== static members ==========

	private static final Logger logger = Logger.getLogger(PublisherNode.class.getName());

	// ========== members ==========

	@Getter private final GraphName topicName;
	@Getter private final String topicTypeStr;
	private volatile Publisher<T> publisher;
	private final Object publisherReady;
	private int handlerCount;

	public PublisherNode(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		this.topicName = topicName;
		topicTypeStr = TopicManager.getTopicTypeStrFromTopicType(topicType);
		publisherReady = new Object();
		handlerCount = 0;

		NodeManager.executeNode(this);
		synchronized (publisherReady) {
			try {
				publisherReady.wait();
			}
			catch (InterruptedException e) {
				throw new RuntimeException();
			}
		}
	}

	@Override
	public GraphName getDefaultNodeName() {
		return Name.getPublisherNodeName(topicName);
	}

	@Override
	public void onStart(ConnectedNode connectedNode) {
		logger.info(String.format("Publisher node starting: %s @ %s", connectedNode.getName(), connectedNode.getUri()));
		publisher = connectedNode.newPublisher(topicName, topicTypeStr);
		synchronized (publisherReady) {
			publisherReady.notify();
		}
	}

	@Override
	public void onShutdown(Node node) {
		logger.info(String.format("Publisher node shutting down: %s @ %s", node.getName(), node.getUri()));
	}

	@Override
	public void onShutdownComplete(Node node) {
		logger.info(String.format("Publisher node shutting down complete: %s @ %s", node.getName(), node.getUri()));
	}

	@Override
	public void onError(Node node, Throwable throwable) {
		logger.severe(String.format("Publisher node %s at %s error: %s", node.getName(), node.getUri(), throwable));
		// node will shut down after returning
	}

	public void publish(@NonNull T message) {
		publisher.publish(message);
	}

	public T newMessage() {
		return publisher.newMessage();
	}

	public PublisherHandler<T> createHandler() {
		++ handlerCount;
		return new PublisherHandler<>(this);
	}

	public void returnHandler(@NonNull PublisherHandler<T> handler) {
		-- handlerCount;
	}

	public int getHandlerCount() {
		return handlerCount;
	}

}
