package com.ericwen229.sample;

import com.ericwen229.node.TopicSubscribeHandler;
import com.ericwen229.topic.TopicManager;
import com.ericwen229.util.Config;
import org.ros.namespace.GraphName;

public class Listener {

	public static void main(String[] args) {
		// load config
		Config.loadConfig(args[0]);

		// create handler
		TopicSubscribeHandler<std_msgs.String> subscribeHandler =
				TopicManager.subscribeToTopic(GraphName.of("/foo"), std_msgs.String.class);

		// subscribe
		subscribeHandler.subscribe(msg -> System.out.println(String.format("received %s", msg.getData())));

		// run for a while
		try {
			Thread.sleep(5000);
		}
		catch (InterruptedException e) {}

		// close handler
		subscribeHandler.close();

		// exit
		System.exit(0);
	}

}
