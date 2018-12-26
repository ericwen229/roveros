package com.ericwen229.topic;

import com.ericwen229.node.NodeManager;
import com.ericwen229.node.PublisherNodeHandler;
import com.ericwen229.node.SubscriberNodeHandler;
import lombok.NonNull;
import org.ros.internal.message.Message;
import org.ros.namespace.GraphName;

public class TopicManager {

	public static <T extends Message> PublisherNodeHandler<T> publishOnTopic(
			@NonNull GraphName topicName,
			@NonNull Class<T> topicType
	) {
		return NodeManager.acquirePublisherNodeHandler(topicName, topicType);
	}

	public static <T extends Message> SubscriberNodeHandler<T> subscribeToTopic(
			@NonNull GraphName topicName,
			@NonNull Class<T> topicType
	) {
		return NodeManager.acquireSubscriberNodeHandler(topicName, topicType);
	}

	public static <T extends Message> String topicTypeObjectToTopicTypeStr(@NonNull Class<T> topicType) {
		try {
			return (String) topicType.getField("_TYPE").get(null);
		}
		catch (NoSuchFieldException e) {
			throw new RuntimeException(
					String.format("Static field \"_TYPE\" missing in topic type %s", topicType));
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(
					String.format("Cannot access field \"_TYPE\" in topic type %s", topicType));
		}
	}

}
