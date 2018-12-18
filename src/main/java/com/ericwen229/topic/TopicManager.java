package com.ericwen229.topic;

import lombok.NonNull;
import org.ros.internal.message.Message;
import org.ros.namespace.GraphName;

public class TopicManager {

	public static <T extends Message> PublisherHandler<T> publishOnTopic(@NonNull GraphName topicName) {
		return null;
	}

	public static <T extends Message> void subscribeToTopic(@NonNull GraphName topicName, SubscriberHandler<T> subscriberHandler) {
	}

}
