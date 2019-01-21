package com.ericwen229.example;

import com.ericwen229.node.NodeManager;
import com.ericwen229.node.TopicSubscribeHandler;
import com.ericwen229.topic.TopicManager;
import com.ericwen229.util.Config;
import org.ros.namespace.GraphName;

/**
 * An example of implementation of listener on topic of custom type.
 */
public class ListenerCustom {

	/**
	 * Main.
	 *
	 * @param args arguments where configuration file path is expected
	 */
	public static void main(String[] args) {
		// config
		Config.loadConfig(args[0]);
		NodeManager.config(Config.getPropertyAsString("host"), Config.getPropertyAsString("masterURI"));

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
