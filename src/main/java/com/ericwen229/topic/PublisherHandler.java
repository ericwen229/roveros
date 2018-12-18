package com.ericwen229.topic;

import lombok.NonNull;
import org.ros.internal.message.Message;

public interface PublisherHandler<T extends Message> {

	void publish(@NonNull T message);
	T newMessage();
	boolean isReady();

}
