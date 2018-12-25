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

	private static final Logger logger = Logger.getLogger(PublisherNode.class.getName());

	private final GraphName topicName;
	@Getter private final String topicTypeStr;
	private volatile Publisher<T> publisher;
	private Object publisherReady = new Object();
	private int handlerCount;

	public PublisherNode(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		this.topicName = topicName;
		topicTypeStr = TopicManager.getTopicTypeStrFromTopicType(topicType);
		handlerCount = 0;

		NodeManager.executeNode(this);
		synchronized (publisherReady) {
			try {
				publisherReady.wait();
			}
			catch (InterruptedException e) {
				// TODO: exception handling
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
		publisher = null;
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
		// TODO: exception handling
		publisher.publish(message);
	}

	public T newMessage() {
		// TODO: exception handling
		return publisher.newMessage();
	}

	public PublisherHandler<T> createHandler() {
		++ handlerCount;
		return new PublisherHandler<>(this);
	}

	public void returnHandler(@NonNull PublisherHandler<T> handler) {
		-- handlerCount;
		// TODO: handler check
	}

}
