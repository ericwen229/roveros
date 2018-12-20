package com.ericwen229.topic;

import com.ericwen229.node.NodeManager;
import com.ericwen229.util.Config;
import lombok.NonNull;
import org.ros.exception.RosMessageRuntimeException;
import org.ros.internal.message.Message;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
			// IMPORTANT: volatile is necessary here
			// otherwise the handler can't know publisher is ready
			private volatile Publisher<T> publisher = null;

			{ // constructor code
				NodeManager.executeNode(new NodeMain() {
					@Override
					public GraphName getDefaultNodeName() {
						// TODO: internal name management
						return GraphName.of("foo");
					}

					@Override
					public void onStart(ConnectedNode connectedNode) {
						try {
							// TODO
							publisher = connectedNode.newPublisher(topicName, topicTypeStr);
						}
						catch (RosMessageRuntimeException e) {
							// TODO: exception handling
						}
					}

					@Override
					public void onShutdown(Node node) {
						publisher.shutdown();
						publisher = null;
					}

					@Override
					public void onShutdownComplete(Node node) {
					}

					@Override
					public void onError(Node node, Throwable throwable) {
						node.shutdown();
					}
				});
			}

			@Override
			public void publish(@NonNull T message) {
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
			public String getTopicTypeStr() {
				return topicTypeStr;
			}
		};
	}

	public static <T extends Message> void subscribeToTopic(@NonNull GraphName topicName, @NonNull Class<T> topicType, @NonNull SubscriberHandler<T> subscriberHandler) {
		String topicTypeStr = getTopicTypeStrFromTopicType(topicType);

		NodeManager.executeNode(new NodeMain() {
			@Override
			public GraphName getDefaultNodeName() {
				// TODO: internal name management
				return GraphName.of("bar");
			}

			@Override
			public void onStart(ConnectedNode connectedNode) {
				try {
					Subscriber<T> subscriber = connectedNode.newSubscriber(topicName, topicTypeStr);
					subscriber.addMessageListener(subscriberHandler::accept);
				}
				catch (RosMessageRuntimeException e) {
					logger.severe(String.format("Subscription to topic %s with type %s exception: %s", topicName, topicTypeStr, e));
					throw new RuntimeException();
				}
			}

			@Override
			public void onShutdown(Node node) {
				logger.info(String.format("Subscription node %s shutting down: %s", node.getName(), node.getUri()));
			}

			@Override
			public void onShutdownComplete(Node node) {
			}

			@Override
			public void onError(Node node, Throwable throwable) {
				node.shutdown();
				throw new RuntimeException();
			}
		});
	}

	// ========== sample program ==========

	public static void main(String[] args) {
		// load config
		Config.loadConfig(args[0]);

		// create thread pool
		ExecutorService executor = Executors.newCachedThreadPool();

		// publisher
		executor.execute(() -> {
			// create handler
			PublisherHandler<std_msgs.String> handler = publishOnTopic(GraphName.of("/foo"), std_msgs.String.class);

			// wait for handler to get ready
			while (!handler.isReady()) {}

			// prepare and publish message
			std_msgs.String msg = handler.newMessage();
			for (int i = 0; i < 1000; i++) {
				msg.setData(String.format("hello #%d", i));
				System.out.println(String.format("sent %s", msg.getData()));
				handler.publish(msg);
				try {
					Thread.sleep(1000);
				}
				catch (InterruptedException e) {}
			}
		});

		// subscriber
		executor.execute(() -> {
			// create handler
			SubscriberHandler<std_msgs.String> handler = msg -> System.out.println(String.format("received %s", msg.getData()));

			// subscribe
			subscribeToTopic(GraphName.of("/foo"), std_msgs.String.class, handler);
		});
	}

}
