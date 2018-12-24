package com.ericwen229.topic;

import org.ros.internal.message.Message;

import java.util.function.Consumer;

/**
 * A simple interface of message subscribing.
 *
 * @param <T> topic type
 */
public interface SubscriberHandler<T extends Message> {

	/**
	 * Subscribe to a topic.
	 *
	 * @param consumer callback invoked on message arrival
	 */
	void subscribe(Consumer<T> consumer);

	/**
	 * Unsubscribe to a topic.
	 *
	 * @param consumer callbak invoked on message arrival
	 */
	void unsubscribe(Consumer<T> consumer);

	/**
	 * Close the handler along with all its resources (node, subscriber, etc).
	 */
	void close();

	/**
	 * Get runtime type of topic.
	 *
	 * @return type of topic
	 */
	Class<T> getTopicType();

}
