package com.ericwen229.node;

import lombok.NonNull;
import org.ros.internal.message.Message;

public class PublisherNodeHandler<T extends Message> {

	private final PublisherNode<T> publisherNode;

	PublisherNodeHandler(@NonNull PublisherNode<T> publisherNode) {
		this.publisherNode = publisherNode;
	}

	public boolean isReady() {
		return publisherNode.isReady();
	}

	public T newMessage() {
		return publisherNode.newMessage();
	}

	public void publish(@NonNull T message) {
		publisherNode.publish(message);
	}

	public void blockUntilReady() {
		publisherNode.blockUntilReady();
	}

}
