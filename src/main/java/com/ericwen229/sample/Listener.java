package com.ericwen229.sample;

import com.ericwen229.node.SubscriberNodeHandler;
import com.ericwen229.topic.TopicManager;
import com.ericwen229.util.Config;
import org.ros.namespace.GraphName;

public class Listener {

	public static void main(String[] args) {
		Config.loadConfig(args[0]);

		SubscriberNodeHandler<std_msgs.String> subscriberHandler =
				TopicManager.subscribeToTopic(GraphName.of("/foo"), std_msgs.String.class);

		subscriberHandler.subscribe(msg -> System.out.println(String.format("received %s", msg.getData())));
	}

}
