package com.ericwen229.topic;

import com.ericwen229.node.NodeManager;
import com.ericwen229.node.TopicPublishHandler;
import com.ericwen229.node.TopicSubscribeHandler;
import lombok.NonNull;
import org.ros.internal.message.Message;
import org.ros.namespace.GraphName;

/**
 * This class provides topic publish/subscribe API.
 */
public class TopicManager {

	/**
	 * Create a publish handler, with which user program can create and publish messages.
	 *
	 * @param topicName name of topic to publish on
	 * @param topicType class object of topic type
	 * @param <T> type of topic to publish on
	 * @return topic publish handler
	 */
	public static <T extends Message> TopicPublishHandler<T> publishOnTopic(
			@NonNull GraphName topicName,
			@NonNull Class<T> topicType
	) {
		return NodeManager.acquireTopicPublishHandler(topicName, topicType);
	}

	/**
	 * Create a subscribe handler, with which user program can subscribe to topic.
	 *
	 * @param topicName name of topic to subscribe to
	 * @param topicType class object of topic type
	 * @param <T> type of topic to subscribe to
	 * @return topic subscribe handler
	 */
	public static <T extends Message> TopicSubscribeHandler<T> subscribeToTopic(
			@NonNull GraphName topicName,
			@NonNull Class<T> topicType
	) {
		return NodeManager.acquireTopicSubscribeHandler(topicName, topicType);
	}

	/**
	 * Retrieve static field _TYPE from given class object.
	 *
	 * @param topicType class object of topic type
	 * @param <T> type of topic
	 * @return value of static field _TYPE
	 */
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
