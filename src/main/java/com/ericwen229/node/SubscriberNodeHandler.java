package com.ericwen229.node;

import lombok.NonNull;
import org.ros.internal.message.Message;

import java.util.function.Consumer;

public class SubscriberNodeHandler<T extends Message> {

	private final SubscriberNode<T> subscriberNode;

	SubscriberNodeHandler(@NonNull SubscriberNode<T> subscriberNode) {
		this.subscriberNode = subscriberNode;
	}

	public void subscribe(@NonNull Consumer<T> consumer) {
		subscriberNode.subscribe(consumer);
	}

	public void unsubscribe(@NonNull Consumer<T> consumer) {
		subscriberNode.unsubscribe(consumer);
	}

}
