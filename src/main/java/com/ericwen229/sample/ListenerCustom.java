package com.ericwen229.sample;

import com.ericwen229.node.SubscriberNodeHandler;
import com.ericwen229.topic.TopicManager;
import com.ericwen229.util.Config;
import org.ros.namespace.GraphName;

public class ListenerCustom {

	public static void main(String[] args) {
		Config.loadConfig(args[0]);

		SubscriberNodeHandler<CustomMsg> handler =
				TopicManager.subscribeToTopic(GraphName.of("/foo"), CustomMsg.class);

		handler.subscribe(msg -> System.out.println(String.format("received %s - %d", msg.getFoo(), msg.getBar())));
	}

}
