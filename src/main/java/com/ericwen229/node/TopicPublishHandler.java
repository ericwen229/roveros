package com.ericwen229.node;

import lombok.NonNull;
import org.ros.internal.message.Message;

/**
 * This class implements handlers used by user programs to create and
 * publish messages.
 *
 * @param <T> type of topic on which node is publishing
 */
public class TopicPublishHandler<T extends Message> {

	/**
	 * Publisher node associated with handler.
	 */
	private final PublisherNode<T> publisherNode;

	/**
	 * True if handler's been closed by invoking {@link #close()}.
	 */
	private boolean isHandlerClosed = false;

	/**
	 * Create a handler associated with given node
	 *
	 * @param publisherNode publisher node with which handler newly created is associated
	 */
	TopicPublishHandler(@NonNull PublisherNode<T> publisherNode) {
		this.publisherNode = publisherNode;
	}

	/**
	 * Close handler if handler isn't closed before GC.
	 */
	@Override
	protected void finalize() {
		if (!isHandlerClosed) {
			close();
		}
	}

	/**
	 * Return publisher node associated with handler.
	 *
	 * @return publisher node associated with handler
	 */
	PublisherNode<T> getPublisherNode() {
		return publisherNode;
	}

	/**
	 * True if handler is ready for publishing.
	 *
	 * @return true if handler is ready for publishing
	 */
	synchronized public boolean isReady() {
		checkHandlerNotClosed();
		return publisherNode.isReady();
	}

	/**
	 * Create a new message object.
	 *
	 * @return object newly created
	 */
	synchronized public T newMessage() {
		checkHandlerNotClosed();
		return publisherNode.newMessage();
	}

	/**
	 * Publish given message.
	 *
	 * @param message message to publish
	 */
	synchronized public void publish(@NonNull T message) {
		checkHandlerNotClosed();
		publisherNode.publish(message);
	}

	/**
	 * Block current thread until handler is ready for publishing.
	 */
	synchronized public void blockUntilReady() {
		checkHandlerNotClosed();
		publisherNode.blockUntilReady();
	}

	/**
	 * Close handler. Handler won't be able to be used handler after this.
	 */
	synchronized public void close() {
		checkHandlerNotClosed();
		isHandlerClosed = true;
		NodeManager.returnTopicPublishHandler(this);
	}

	/**
	 * Throw an exception if handler's already been closed.
	 */
	private void checkHandlerNotClosed() {
		if (isHandlerClosed)
			throw new RuntimeException("Publisher handler has been closed");
	}

}
