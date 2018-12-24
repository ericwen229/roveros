package com.ericwen229.topic;

import com.ericwen229.node.NodeManager;
import com.ericwen229.util.Name;
import lombok.NonNull;
import org.ros.internal.message.Message;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * High-level APIs for topic publish/subscribe.
 */
public class TopicManager {

	// ========== static members ==========

	/**
	 * Logger of the class.
	 */
	private static final Logger logger = Logger.getLogger(TopicManager.class.getName());

	private static final HashMap<GraphName, PublisherHandler> topicNameToPublisherHandler = new HashMap<>();

	private static final HashMap<GraphName, SubscriberHandler> topicNameToSubscriberHandler = new HashMap<>();



	// ========== static methods ==========

	/**
	 * Creates a publisher handler, with which one can publish on a specified topic by invoking its method.
	 *
	 * @param topicName name of topic to publish on
	 * @param topicType class object of topic type, used to retrieve the type string statically defined in topic type
	 * @param <T> topic type
	 * @return created publisher handler
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Message> PublisherHandler<T> publishOnTopic(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		String topicTypeStr = getTopicTypeStrFromTopicType(topicType);
		PublisherHandler handler = topicNameToPublisherHandler.getOrDefault(topicName, null);

		if (handler == null) {
			// create a new handler
			handler = new PublisherHandler<T>() {

				// ========== members ==========

				// IMPORTANT: volatile is necessary here
				// it will be set in a different thread (in which onStart is invoked)
				private volatile Publisher<T> publisher = null;

				private final NodeMain node = new NodeMain() {
					@Override
					public GraphName getDefaultNodeName() {
						return Name.getPublisherNodeName(topicName);
					}

					@Override
					public void onStart(ConnectedNode connectedNode) {
						// create publisher
						// (publisher handler is going to use it)
						publisher = connectedNode.newPublisher(topicName, topicTypeStr);
					}

					@Override
					public void onShutdown(Node node) {
						logger.info(String.format("Publisher node %s at %s shutting down", node.getName(), node.getUri()));
						publisher = null; // it will be shutdown automatically, just remove reference to it
					}

					@Override
					public void onShutdownComplete(Node node) {
					}

					@Override
					public void onError(Node node, Throwable throwable) {
						logger.severe(String.format("Publisher node %s at %s error: %s", node.getName(), node.getUri(), throwable));
						// node will shutdown after returning
					}
				};

				// ========== constructor ==========

				{
					NodeManager.executeNode(node);
				}

				// ========== methods ==========

				@Override
				public void publish(@NonNull T message) {
					if (!isReady()) {
						throw new RuntimeException("Publisher not ready");
					}
					publisher.publish(message);
				}

				@Override
				public T newMessage() {
					if (!isReady()) {
						throw new RuntimeException("Publisher not ready");
					}
					return publisher.newMessage();
				}

				@Override
				public boolean isReady() {
					return publisher != null;
				}

				@Override
				public void close() {
					NodeManager.shutdownNode(node);
				}

				@Override
				public String getTopicTypeStr() {
					return topicTypeStr;
				}

			};

			topicNameToPublisherHandler.put(topicName, handler);
			return (PublisherHandler<T>) handler;
		}
		else {
			// there is a handler already
			// same class?
			if (handler.getTopicTypeStr().equals(topicTypeStr)) {
				// okay
				// just return it
				return (PublisherHandler<T>) handler;
			}
			else {
				// error
				throw new IllegalArgumentException(
						String.format("Trying to publish on topic %s again with a different topic type\nOld type: %s\nNew type: %s",
								topicName, handler.getTopicTypeStr(), topicTypeStr));
			}
		}
	}

	/**
	 * Creates a subscriber handler, with which one can subscribe to a given topic by giving it a callback.
	 * A node will be automatically created and will subscribe to the given topic.
	 *
	 * @param topicName name of topic to subscribe to
	 * @param topicType class object of topic type, used to retrieve the type string statically defined in topic type
	 * @param <T> topic type
	 * @return created subscriber handler
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Message> SubscriberHandler<T> subscribeToTopic(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		String topicTypeStr = getTopicTypeStrFromTopicType(topicType);
		SubscriberHandler handler = topicNameToSubscriberHandler.getOrDefault(topicName, null);

		if (handler == null) {
			handler = new SubscriberHandler<T>() {

				// ========== members ==========

				private MessageListener<T> messageConsumer = this::accept;
				private final List<Consumer<T>> subscribers = Collections.synchronizedList(new ArrayList<>());

				private NodeMain node = new NodeMain() {
					@Override
					public GraphName getDefaultNodeName() {
						return Name.getSubscriberNodeName(topicName);
					}

					@Override
					public void onStart(ConnectedNode connectedNode) {
						Subscriber<T> subscriber = connectedNode.newSubscriber(topicName, topicTypeStr);
						subscriber.addMessageListener(messageConsumer);
					}

					@Override
					public void onShutdown(Node node) {
						logger.info(String.format("Subscriber node %s at %s shutting down", node.getName(), node.getUri()));
					}

					@Override
					public void onShutdownComplete(Node node) {}

					@Override
					public void onError(Node node, Throwable throwable) {
						logger.severe(String.format("Subscriber node %s at %s error: %s", node.getName(), node.getUri(), throwable));
						// node will shutdown after returning
					}
				};

				// ========== constructor ==========

				{
					NodeManager.executeNode(node);
				}

				// ========== methods ==========

				private void accept(@NonNull T message) {
					for (Consumer<T> c: subscribers) {
						c.accept(message);
					}
				}

				@Override
				public void subscribe(@NonNull Consumer<T> consumer) {
					subscribers.add(consumer);
				}

				@Override
				public void unsubscribe(@NonNull Consumer<T> consumer) {
					subscribers.remove(consumer);
				}

				@Override
				public void close() {
					NodeManager.shutdownNode(node);
				}

				@Override
				public String getTopicTypeStr() {
					return topicTypeStr;
				}
			};

			topicNameToSubscriberHandler.put(topicName, handler);
			return (SubscriberHandler<T>) handler;
		}
		else {
			// there is a handler already
			// same class?
			if (handler.getTopicTypeStr().equals(topicTypeStr)) {
				// okay
				// just return it
				return (SubscriberHandler<T>) handler;
			}
			else {
				// error
				throw new IllegalArgumentException(
						String.format("Trying to publish on topic %s again with a different topic type\nOld type: %s\nNew type: %s",
								topicName, handler.getTopicTypeStr(), topicTypeStr));
			}
		}
	}



	// ========== utils ==========

	/**
	 * Retrieve static member "String _TYPE" from the given class object.
	 *
	 * @param topicType class object of topic type
	 * @param <T> topic type
	 * @return type string of topic type
	 */
	private static <T extends Message> String getTopicTypeStrFromTopicType(Class<T> topicType) {
		try {
			// get static field String _TYPE
			// this field is in every interface of message type that extends base message type
			return (String)topicType.getField("_TYPE").get(topicType);
		}
		catch (NoSuchFieldException e) {
			throw new RuntimeException(String.format("Incorrect topic type %s: field _TYPE missing", topicType.getName()));
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(String.format("Incorrect topic type %s: can't access field _TYPE", topicType.getName()));
		}
	}

}
