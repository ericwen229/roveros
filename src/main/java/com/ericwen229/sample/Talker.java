package com.ericwen229.sample;

import com.ericwen229.node.NodeManager;
import com.ericwen229.topic.PublisherHandler;
import com.ericwen229.topic.TopicManager;
import com.ericwen229.util.Config;
import org.ros.namespace.GraphName;

public class Talker {

	public static void main(String[] args) {
		// load config
		Config.loadConfig(args[0]);

		// create handler
		PublisherHandler<std_msgs.String> handler = TopicManager.publishOnTopic(GraphName.of("/foo"), std_msgs.String.class);

		// add shutdown hook for ctrl-c exit
		Runtime.getRuntime().addShutdownHook(new Thread(handler::close));

		// wait for handler to get ready
		while (!handler.isReady()) {}

		// keep firing new messages
		std_msgs.String msg = handler.newMessage();
		for (int i = 0; i < 20; i++) {
			// publish a new message
			msg.setData(String.format("hello #%d", i));
			System.out.println(String.format("sent %s", msg.getData()));
			handler.publish(msg);

			try {
				// wait a second
				Thread.sleep(500);
			}
			catch (InterruptedException e) {}
		}

		// shutdown
		NodeManager.shutdown();
	}

}
