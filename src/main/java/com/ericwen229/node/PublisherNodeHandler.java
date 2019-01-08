package com.ericwen229.node;

import lombok.NonNull;
import org.ros.internal.message.Message;

public class PublisherNodeHandler<T extends Message> {

	private final PublisherNode<T> publisherNode;
	private boolean isHandlerClosed = false;

	PublisherNodeHandler(@NonNull PublisherNode<T> publisherNode) {
		this.publisherNode = publisherNode;
	}

	public boolean isReady() {
		if (isHandlerClosed)
			throw new RuntimeException("Publisher handler has been closed");
		return publisherNode.isReady();
	}

	synchronized public T newMessage() {
		if (isHandlerClosed)
			throw new RuntimeException("Publisher handler has been closed");
		return publisherNode.newMessage();
	}

	synchronized public void publish(@NonNull T message) {
		if (isHandlerClosed)
			throw new RuntimeException("Publisher handler has been closed");
		publisherNode.publish(message);
	}

	synchronized public void blockUntilReady() {
		if (isHandlerClosed)
			throw new RuntimeException("Publisher handler has been closed");
		publisherNode.blockUntilReady();
	}

	synchronized public void close() {
		if (isHandlerClosed)
			throw new RuntimeException("Publisher handler has been closed");
		isHandlerClosed = true;
		publisherNode.returnHandler();
	}

}
