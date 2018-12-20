package com.ericwen229.topic;

import org.ros.internal.message.Message;

public interface PublisherHandler<T extends Message> {

	void publish(T message);
	T newMessage();
	boolean isReady();
	String getTopicType();

}
