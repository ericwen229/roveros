package com.ericwen229.topic;

import com.ericwen229.node.PublisherNode;
import lombok.NonNull;
import org.ros.internal.message.Message;

/**
 * A simple interface of message publishing.
 *
 * @param <T> topic type
 */
public class PublisherHandler<T extends Message> {

	private final PublisherNode<T> publisherNode;

	public PublisherHandler(@NonNull PublisherNode publisherNode) {
		this.publisherNode = publisherNode;
	}

	/**
	 * Publish a message.
	 *
	 * @param message message to publish
	 */
	public void publish(@NonNull T message) {
		publisherNode.publish(message);
	}

	/**
	 * Create a new message instance.
	 *
	 * @return newly created message instance
	 */
	public T newMessage() {
		return publisherNode.newMessage();
	}

	/**
	 * Close the handler along with all its resources (node, publisher, etc).
	 */
	public void close() {
		publisherNode.returnHandler(this);
	}

}
