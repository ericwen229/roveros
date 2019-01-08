package com.ericwen229.node;

import lombok.NonNull;
import org.ros.internal.message.Message;

import java.util.function.Consumer;

public class SubscriberNodeHandler<T extends Message> {

	private final SubscriberNode<T> subscriberNode;
	private volatile boolean isHandlerClosed = false;

	SubscriberNodeHandler(@NonNull SubscriberNode<T> subscriberNode) {
		this.subscriberNode = subscriberNode;
	}

	synchronized public void subscribe(@NonNull Consumer<T> consumer) {
		if (isHandlerClosed)
			throw new RuntimeException("Subscriber handler has been closed");
		subscriberNode.subscribe(consumer);
	}

	synchronized public void unsubscribe(@NonNull Consumer<T> consumer) {
		if (isHandlerClosed)
			throw new RuntimeException("Subscriber handler has been closed");
		subscriberNode.unsubscribe(consumer);
	}

	synchronized public void close() {
		if (isHandlerClosed)
			throw new RuntimeException("Subscriber handler has been closed");
		isHandlerClosed = true;
		subscriberNode.returnHandler();
	}

}
