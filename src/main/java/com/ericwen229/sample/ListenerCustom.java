package com.ericwen229.sample;

import com.ericwen229.node.TopicSubscribeHandler;
import com.ericwen229.topic.TopicManager;
import com.ericwen229.util.Config;
import org.ros.namespace.GraphName;

public class ListenerCustom {

	public static void main(String[] args) {
		// load config
		Config.loadConfig(args[0]);

		// create handler
		TopicSubscribeHandler<CustomMsg> subscribeHandler =
				TopicManager.subscribeToTopic(GraphName.of("/foo"), CustomMsg.class);

		// subscribe
		subscribeHandler.subscribe(msg -> System.out.println(String.format("received %s - %d", msg.getFoo(), msg.getBar())));

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
