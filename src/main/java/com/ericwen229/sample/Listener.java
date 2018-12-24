package com.ericwen229.sample;

import com.ericwen229.topic.SubscriberHandler;
import com.ericwen229.topic.TopicManager;
import com.ericwen229.util.Config;
import org.ros.namespace.GraphName;

public class Listener {

	public static void main(String[] args) {
		// load config
		Config.loadConfig(args[0]);

		// create handler
		SubscriberHandler<std_msgs.String> handler = TopicManager.subscribeToTopic(GraphName.of("/foo"), std_msgs.String.class);

		// add shutdown hook for ctrl-c exit
		Runtime.getRuntime().addShutdownHook(new Thread(handler::close));

		// subscribe
		handler.subscribe(msg -> System.out.println(String.format("received %s", msg.getData())));
	}

}
