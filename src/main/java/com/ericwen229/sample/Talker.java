package com.ericwen229.sample;

import com.ericwen229.node.PublisherNodeHandler;
import com.ericwen229.topic.TopicManager;
import com.ericwen229.util.Config;
import org.ros.namespace.GraphName;

public class Talker {

	public static void main(String[] args) {
		Config.loadConfig(args[0]);

		PublisherNodeHandler<std_msgs.String> publisherHandler =
				TopicManager.publishOnTopic(GraphName.of("/foo"), std_msgs.String.class);
		publisherHandler.blockUntilReady();

		std_msgs.String msg = publisherHandler.newMessage();
		for (int i = 0; i < 10; i++) {
			msg.setData(Integer.toString(i));
			publisherHandler.publish(msg);
			System.out.println(String.format("published %s", msg.getData()));

			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e) {}
		}

		publisherHandler.close();

		System.exit(0);
	}

}
