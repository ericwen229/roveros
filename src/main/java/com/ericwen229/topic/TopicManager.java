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

public class TopicManager {

	private static final Logger logger = Logger.getLogger(TopicManager.class.getName());

	private static <T extends Message> String getTopicTypeStrFromTopicType(Class<T> topicType) {
		try {
			return (String)topicType.getField("_TYPE").get(topicType);
		}
		catch (NoSuchFieldException e) {
			throw new RuntimeException(String.format("Incorrect topic type %s: field _TYPE missing", topicType.getName()));
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(String.format("Incorrect topic type %s: can't access field _TYPE"));
		}
	}

	public static <T extends Message> PublisherHandler<T> publishOnTopic(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		String topicTypeStr = getTopicTypeStrFromTopicType(topicType);

		return new PublisherHandler<T>() {

			// ========== members ==========

			private final NodeMain node = new NodeMain() {
				@Override
				public GraphName getDefaultNodeName() {
					// TODO: internal name management
					return GraphName.of(String.format("/roveros/publish%s", topicName));
				}

				@Override
				public void onStart(ConnectedNode connectedNode) {
					publisher = connectedNode.newPublisher(topicName, topicTypeStr);
				}

				@Override
				public void onShutdown(Node node) {
					publisher = null;
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

			// IMPORTANT: volatile is necessary here
			// otherwise the handler can't know publisher is ready
			private volatile Publisher<T> publisher = null;

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

	public static <T extends Message> SubscriberHandler<T> subscribeToTopic(@NonNull GraphName topicName, @NonNull Class<T> topicType) {
		String topicTypeStr = getTopicTypeStrFromTopicType(topicType);

		return new SubscriberHandler<T>() {

			// ========== members ==========

			private MessageListener<T> messageConsumer = this::accept;

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

			private final List<Consumer<T>> subscribers = Collections.synchronizedList(new ArrayList<>());

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

	// ========== sample program ==========

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

			// prepare and publish message
			std_msgs.String msg = handler.newMessage();
			int i = 0;
			while (!Thread.currentThread().isInterrupted()) {
				msg.setData(String.format("hello #%d", i++));
				System.out.println(String.format("sent %s", msg.getData()));
				handler.publish(msg);
				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}

			handler.close();
			return null;
		});

		// subscriber
		Future<Void> subscriberFuture = executor.submit(() -> {
			// create handler
			SubscriberHandler<std_msgs.String> handler = subscribeToTopic(GraphName.of("/foo"), std_msgs.String.class);

			// subscribe
			handler.subscribe(msg -> System.out.println(String.format("received %s", msg.getData())));

			while (!Thread.currentThread().isInterrupted());

			handler.close();
			return null;
		});

		try {
			Thread.sleep(5000);
		}
		catch (InterruptedException e) {}

		publisherFuture.cancel(true);
		subscriberFuture.cancel(true);
	}

}
