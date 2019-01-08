package com.ericwen229.node;

import com.ericwen229.topic.TopicManager;
import com.ericwen229.util.Name;
import lombok.Getter;
import lombok.NonNull;
import org.ros.internal.message.Message;
import org.ros.internal.node.topic.SubscriberIdentifier;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.PublisherListener;

class PublisherNode<T extends Message> implements NodeMain {

	@Getter private final GraphName topicName;
	@Getter private final Class<T> topicType;
	private Publisher<T> publisher;
	private volatile boolean isPublisherReady = false;
	private final Object publisherReadyNotifier = new Object();
	private int handlerCount = 0;

	PublisherNode(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		this.topicName = topicName;
		this.topicType = topicType;
	}

	@Override
	public GraphName getDefaultNodeName() {
		return Name.getPublisherNodeName(topicName);
	}

	@Override
	public void onStart(ConnectedNode connectedNode) {
		isPublisherReady = false;
		connectedNode.getLog().info(
				String.format(
						"Publisher node %s at %s starting",
						getDefaultNodeName(),
						connectedNode.getUri()));

		publisher = connectedNode.newPublisher(topicName, TopicManager.topicTypeObjectToTopicTypeStr(topicType));
		publisher.addListener(new PublisherListener<T>() {
			@Override public void onNewSubscriber(Publisher<T> publisher, SubscriberIdentifier subscriberIdentifier) {}
			@Override public void onShutdown(Publisher<T> publisher) {}
			@Override public void onMasterRegistrationSuccess(Publisher<T> tPublisher) {
				isPublisherReady = true;
				synchronized (publisherReadyNotifier) {
					publisherReadyNotifier.notifyAll();
				}
			}
			@Override public void onMasterRegistrationFailure(Publisher<T> tPublisher) {}
			@Override public void onMasterUnregistrationSuccess(Publisher<T> tPublisher) {}
			@Override public void onMasterUnregistrationFailure(Publisher<T> tPublisher) {}
		});
	}

	@Override
	public void onShutdown(Node node) {
		isPublisherReady = false;
		node.getLog().info(
				String.format(
						"Publisher node %s at %s shutting down",
						getDefaultNodeName(),
						node.getUri()));
	}

	@Override
	public void onShutdownComplete(Node node) {
		isPublisherReady = false;
		node.getLog().info(
				String.format(
						"Publisher node %s at %s shut down complete",
						getDefaultNodeName(),
						node.getUri()));
	}

	@Override
	public void onError(Node node, Throwable throwable) {
		isPublisherReady = false;
		node.getLog().fatal(
				String.format(
						"Publisher node %s at %s error: %s",
						getDefaultNodeName(),
						node.getUri(),
						throwable));
		System.exit(-1);
	}

	PublisherNodeHandler<T> createHandler() {
		handlerCount ++;
		return new PublisherNodeHandler<>(this);
	}

	void returnHandler() {
		handlerCount --;
		if (handlerCount == 0) {
			NodeManager.shutdownPublisherNode(this);
		}
	}

	boolean isReady() {
		return isPublisherReady;
	}

	T newMessage() {
		if (!isReady()) {
			throw new RuntimeException("Publisher node not ready");
		}
		return publisher.newMessage();
	}

	void publish(@NonNull T message) {
		if (!isReady()) {
			throw new RuntimeException("Publisher node not ready");
		}
		publisher.publish(message);
	}

	void blockUntilReady() {
		synchronized (publisherReadyNotifier) {
			if (!isReady()) {
				try {
					publisherReadyNotifier.wait();
				}
				catch (InterruptedException e) {
					throw new RuntimeException("Unexpected interruption");
				}
			}
		}
	}

}
