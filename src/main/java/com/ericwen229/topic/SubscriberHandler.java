package com.ericwen229.topic;

import org.ros.internal.message.Message;

import java.util.function.Consumer;

public interface SubscriberHandler<T extends Message> {

	void close();
	void subscribe(Consumer<T> consumer);
	void unsubscribe(Consumer<T> consumer);

}
