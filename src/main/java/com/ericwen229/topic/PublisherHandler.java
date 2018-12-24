package com.ericwen229.topic;

import org.ros.internal.message.Message;

/**
 * A simple interface of message publishing.
 *
 * @param <T> topic type
 */
public interface PublisherHandler<T extends Message> {

	/**
	 * Publish a message.
	 *
	 * @param message message to publish
	 */
	void publish(T message);

	/**
	 * Create a new message instance.
	 *
	 * @return newly created message instance
	 */
	T newMessage();

	/**
	 * True if ready for publish
	 * (node's been created, connected to master and publisher's been created).
	 *
	 * @return true if ready for publish
	 */
	boolean isReady();

	/**
	 * Close the handler along with all its resources (node, publisher, etc).
	 */
	void close();

	/**
	 * Get type string of topic.
	 *
	 * @return type string of topic
	 */
	String getTopicTypeStr();

}
