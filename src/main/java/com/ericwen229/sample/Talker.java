package com.ericwen229.sample;

import com.ericwen229.node.TopicPublishHandler;
import com.ericwen229.topic.TopicManager;
import com.ericwen229.util.Config;
import org.ros.namespace.GraphName;

public class Talker {

	public static void main(String[] args) {
		// load config
		Config.loadConfig(args[0]);

		// create handler
		TopicPublishHandler<std_msgs.String> publishHandler =
				TopicManager.publishOnTopic(GraphName.of("/foo"), std_msgs.String.class);

		// wait until ready
		publishHandler.blockUntilReady();

		// create message
		std_msgs.String msg = publishHandler.newMessage();
		for (int i = 0; i < 20; i++) {
			// edit message
			msg.setData(Integer.toString(i));

			// publish message
			publishHandler.publish(msg);
			System.out.println(String.format("published %s", msg.getData()));

			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {}
		}

		// close handler
		publishHandler.close();

		// exit
		System.exit(0);
	}

}
