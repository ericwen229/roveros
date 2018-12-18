package com.ericwen229.topic;

import com.ericwen229.node.NodeExecutor;
import com.ericwen229.util.Config;
import lombok.NonNull;
import org.ros.internal.message.Message;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class TopicManager {

	public static <T extends Message> PublisherHandler<T> publishOnTopic(@NonNull GraphName topicName, @NonNull String topicType) {
		return new PublisherHandler<T>() {
			// volatile is necessary here
			// otherwise the handler can't know publisher is ready
			private volatile Publisher<T> publisher = null;

			{
				NodeExecutor.executeNode(new NodeMain() {
					@Override
					public GraphName getDefaultNodeName() {
						return GraphName.of("foo");
					}

					@Override
					public void onStart(ConnectedNode connectedNode) {
						publisher = connectedNode.newPublisher(topicName, topicType);
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
		};
	}

	public static <T extends Message> void subscribeToTopic(@NonNull GraphName topicName, @NonNull SubscriberHandler<T> subscriberHandler) {
	}

	// sample program

	public static void main(String[] args) {
		try {
			Config.loadConfig(new FileInputStream(args[0]));
		} catch (FileNotFoundException e) {
			// TODO
		}

		PublisherHandler<std_msgs.String> handler = publishOnTopic(GraphName.of("/foo"), std_msgs.String._TYPE);
		while (!handler.isReady()) {}
		std_msgs.String msg = handler.newMessage();
		msg.setData("nee");
		while (true) {
			handler.publish(msg);
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				break;
			}
		}
	}

}
