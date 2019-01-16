package com.ericwen229.node;

import lombok.NonNull;
import org.ros.internal.message.Message;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * This class implements handlers used by user programs to subscribe
 * to topics.
 *
 * @param <T> type of topic to which node is subscribed
 */
public class TopicSubscribeHandler<T extends Message> {

	/**
	 * Subscriber node associated with handler.
	 */
	private final SubscriberNode<T> subscriberNode;

	/**
	 * True if handler's been closed by invoking {@link #close()}.
	 */
	private volatile boolean isHandlerClosed = false;

	/**
	 * Topic subscribers (message consumers).
	 */
	private final Set<Consumer<T>> subscribers;

	/**
	 * Create a handler associated with given node
	 *
	 * @param subscriberNode subscriber node with which handler newly created is associated
	 */
	TopicSubscribeHandler(@NonNull SubscriberNode<T> subscriberNode) {
		this.subscriberNode = subscriberNode;
		subscribers = new HashSet<>();
	}

	/**
	 * Close handler if handler isn't closed before GC.
	 */
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if (!isHandlerClosed) {
			close();
		}
	}

	/**
	 * Return subscriber node associated with handler.
	 *
	 * @return subscriber node associated with handler
	 */
	SubscriberNode<T> getSubscriberNode() {
		return subscriberNode;
	}

	/**
	 * Subscribe to topic.
	 *
	 * @param consumer message receival callback
	 */
	synchronized public void subscribe(@NonNull Consumer<T> consumer) {
		checkHandlerNotClosed();
		subscribers.add(consumer);
	}

	/**
	 * Unsubscribe to topic.
	 *
	 * @param consumer message receival callback
	 */
	synchronized public void unsubscribe(@NonNull Consumer<T> consumer) {
		checkHandlerNotClosed();
		subscribers.remove(consumer);
	}

	/**
	 * Close handler. Handler won't be able to be used after this.
	 */
	public void close() {
		checkHandlerNotClosed();
		isHandlerClosed = true;
		NodeManager.returnTopicSubscribeHandler(this);
	}

	/**
	 * Invoked when there's a new message.
	 *
	 * @param message message received
	 */
	void accept(@NonNull T message) {
		for (Consumer<T> subscriber: subscribers) {
			subscriber.accept(message);
		}
	}

	/**
	 * Throw an exception if handler's already been closed.
	 */
	private void checkHandlerNotClosed() {
		if (isHandlerClosed)
			throw new RuntimeException("Subscriber handler has been closed");
	}

}
