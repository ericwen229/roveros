package com.ericwen229.topic;

import com.ericwen229.node.NodeManager;
import com.ericwen229.util.Config;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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



	// ========== static methods ==========

	/**
	 * Creates a publisher handler, with which one can publish on a specified topic by invoking its method.
	 *
	 * @param topicName name of topic to publish on
	 * @param topicType class object of topic type, used to retrieve the type string statically defined in topic type
	 * @param <T> topic type
	 * @return created publisher handler
	 */
	public static <T extends Message> PublisherHandler<T> publishOnTopic(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		String topicTypeStr = getTopicTypeStrFromTopicType(topicType);
		return new PublisherHandler<T>() {

			// ========== members ==========

			// IMPORTANT: volatile is necessary here
			// it will be set in a different thread (in which onStart is invoked)
			private volatile Publisher<T> publisher = null;

			private final NodeMain node = new NodeMain() {
				@Override
				public GraphName getDefaultNodeName() {
					// TODO: name management
					return GraphName.of(String.format("/roveros/publish%s", topicName));
				}

				@Override
				public void onStart(ConnectedNode connectedNode) {
					// create publisher
					// (publisher handler is going to use it)
					publisher = connectedNode.newPublisher(topicName, topicTypeStr);
				}

				@Override
				public void onShutdown(Node node) {
					publisher = null; // it will be shutdown automatically, just remove reference to it
				}

				@Override
				public void onShutdownComplete(Node node) {
				}

				@Override
				public void onError(Node node, Throwable throwable) {
					// TODO: exception handling
					node.shutdown();
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
					// TODO: exception handling
					throw new RuntimeException();
				}
				publisher.publish(message);
			}

			@Override
			public T newMessage() {
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

		};
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
	public static <T extends Message> SubscriberHandler<T> subscribeToTopic(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		String topicTypeStr = getTopicTypeStrFromTopicType(topicType);
		return new SubscriberHandler<T>() {

			// ========== members ==========

			private MessageListener<T> messageConsumer = this::accept;
			private final List<Consumer<T>> subscribers = Collections.synchronizedList(new ArrayList<>());

			private NodeMain node = new NodeMain() {
				@Override
				public GraphName getDefaultNodeName() {
					// TODO: name management
					return GraphName.of(String.format("/roveros/subscribe%s", topicName));
				}

				@Override
				public void onStart(ConnectedNode connectedNode) {
					Subscriber<T> subscriber = connectedNode.newSubscriber(topicName, topicTypeStr);
					subscriber.addMessageListener(messageConsumer);
				}

				@Override
				public void onShutdown(Node node) {
				}

				@Override
				public void onShutdownComplete(Node node) {
				}

				@Override
				public void onError(Node node, Throwable throwable) {
					// TODO: error handling
					node.shutdown();
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
		};
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



	// ========== a sample talker-listener program ==========
	// TODO: move it somewhere else

	public static void main(String[] args) {
		// load config
		Config.loadConfig(args[0]);

		// create thread pool
		ExecutorService executor = Executors.newCachedThreadPool();

		// publisher
		Future<Void> publisherFuture = executor.submit(() -> {
			// create handler
			PublisherHandler<std_msgs.String> handler = publishOnTopic(GraphName.of("/foo"), std_msgs.String.class);

			// wait for handler to get ready
			while (!handler.isReady()) {}

			// keep firing new messages
			std_msgs.String msg = handler.newMessage();
			int i = 0;
			while (!Thread.currentThread().isInterrupted()) {
				// publish a new message
				msg.setData(String.format("hello #%d", i++));
				System.out.println(String.format("sent %s", msg.getData()));
				handler.publish(msg);

				try {
					// wait a second
					Thread.sleep(500);
				}
				catch (InterruptedException e) {
					// time to stop
					Thread.currentThread().interrupt();
				}
			}

			// close handler
			handler.close();
			return null;
		});

		// subscriber
		Future<Void> subscriberFuture = executor.submit(() -> {
			// create handler
			SubscriberHandler<std_msgs.String> handler = subscribeToTopic(GraphName.of("/foo"), std_msgs.String.class);

			// subscribe
			handler.subscribe(msg -> System.out.println(String.format("received %s", msg.getData())));

			// wait for interrupt
			while (!Thread.currentThread().isInterrupted()) {}

			// close handler
			handler.close();
			return null;
		});

		try {
			// do something for a period of time
			Thread.sleep(5000);
		}
		catch (InterruptedException e) {
			System.out.println("What? Now I don't even get to sleep for a minute, huh?");
		}

		// time to stop
		publisherFuture.cancel(true);
		subscriberFuture.cancel(true);

		// shutdown thread pools (otherwise the program doesn't stop)
		executor.shutdown();
		NodeManager.shutdown();
	}

}
