package com.ericwen229.sample;

import com.ericwen229.node.NodeManager;
import com.ericwen229.topic.SubscriberHandler;
import com.ericwen229.topic.TopicManager;
import com.ericwen229.util.Config;
import org.ros.namespace.GraphName;

public class ListenerCustom {

	// TODO: move this out of project source
	public static void main(String[] args) {
		// load config
		Config.loadConfig(args[0]);

		// create handler
		SubscriberHandler<CustomMsg> handler = TopicManager.subscribeToTopic(GraphName.of("/foo"), CustomMsg.class);

		// add shutdown hook for ctrl-c exit
		Runtime.getRuntime().addShutdownHook(new Thread(handler::close));

		// subscribe
		handler.subscribe(msg -> System.out.println(String.format("received %s - %d", msg.getFoo(), msg.getBar())));

		// sleep then shutdown
		try {
			Thread.sleep(10000);
		}
		catch (InterruptedException e) {}
		NodeManager.shutdown();
	}

}
