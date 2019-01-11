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

/**
 * This class implements ROS nodes that are responsible for publishing
 * messages. Each node is associated with one single topic on which
 * messages are published.
 *
 * @param <T> type of topic on which the node is publishing
 *
 * @see NodeManager
 * @see TopicPublishHandler
 */
class PublisherNode<T extends Message> implements NodeMain {

	/**
	 * Name of topic on which current node is publishing.
	 */
	@Getter private final GraphName topicName;

	/**
	 * Type of topic on which current node is publishing.
	 */
	@Getter private final Class<T> topicType;

	/**
	 * Publisher created on when node launches and used for message creating and publishing.
	 */
	private Publisher<T> publisher;

	/**
	 * True if current node is ready to publish (has successfully registered at master).
	 */
	private volatile boolean isPublisherReady = false;

	/**
	 * An object on which threads wait for publisher to get ready. The threads are notified
	 * when publisher is ready (has successfully registered at master).
	 */
	private final Object publisherReadyNotifier = new Object();

	/**
	 * Number of active handlers of current node.
	 */
	private int handlerCount = 0;

	// ===========
	// constructor
	// ===========

	/**
	 * Create a publisher node associated with given topic of given type.
	 *
	 * @param topicName name of topic on which the node is publishing
	 * @param topicType type of topic on which the node is publishing
	 */
	PublisherNode(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		this.topicName = topicName;
		this.topicType = topicType;
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
		return Name.getPublisherNodeName(topicName);
	}

	/**
	 * Invoked when node has successfully contacted master. Publisher is created
	 * here.
	 *
	 * @param connectedNode node that's successfully contacted master, used
	 *                      as factory of publisher
	 */
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
					// notify waiting threads of readiness of publisher
					publisherReadyNotifier.notifyAll();
				}
			}
			@Override public void onMasterRegistrationFailure(Publisher<T> tPublisher) {}
			@Override public void onMasterUnregistrationSuccess(Publisher<T> tPublisher) {}
			@Override public void onMasterUnregistrationFailure(Publisher<T> tPublisher) {}
		});
	}

	/**
	 * Invoked when node is shutting down.
	 *
	 * @param node node to be shut down
	 */
	@Override
	public void onShutdown(Node node) {
		isPublisherReady = false;
		node.getLog().info(
				String.format(
						"Publisher node %s at %s shutting down",
						getDefaultNodeName(),
						node.getUri()));
	}

	/**
	 * Invoked when node has benn shut down.
	 *
	 * @param node node that's been shut down
	 */
	@Override
	public void onShutdownComplete(Node node) {
		isPublisherReady = false;
		node.getLog().info(
				String.format(
						"Publisher node %s at %s shut down complete",
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
		isPublisherReady = false;
		node.getLog().fatal(
				String.format(
						"Publisher node %s at %s error: %s",
						getDefaultNodeName(),
						node.getUri(),
						throwable));
		System.exit(-1);
	}

	// ==================
	// handler management
	// ==================

	/**
	 * Create a publisher node handler to be used by user program.
	 *
	 * @return publisher node handler created
	 */
	TopicPublishHandler<T> createHandler() {
		handlerCount ++;
		return new TopicPublishHandler<>(this);
	}

	/**
	 * Return (give back) a publisher handler.
	 *
	 * @param handler handler to return
	 */
	void returnHandler(@NonNull TopicPublishHandler handler) {
		handlerCount --;
	}

	/**
	 * Return number of active handlers.
	 *
	 * @return number of active handlers.
	 */
	int getHandlerCount() {
		return handlerCount;
	}

	// =====
	// utils
	// =====

	/**
	 * True if publisher is ready for publishing.
	 *
	 * @return true if publisher is ready for publishing
	 */
	boolean isReady() {
		return isPublisherReady;
	}

	/**
	 * Create a new message.
	 *
	 * @return new message created
	 */
	T newMessage() {
		if (!isReady()) {
			throw new RuntimeException("Publisher node not ready");
		}
		return publisher.newMessage();
	}

	/**
	 * Publish a new message.
	 *
	 * @param message message to publish
	 */
	void publish(@NonNull T message) {
		if (!isReady()) {
			throw new RuntimeException("Publisher node not ready");
		}
		publisher.publish(message);
	}

	/**
	 * Block current thread until publisher is ready.
	 */
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
