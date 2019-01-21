package com.ericwen229.example;

import com.ericwen229.node.NodeManager;
import com.ericwen229.node.TopicPublishHandler;
import com.ericwen229.topic.TopicManager;
import com.ericwen229.util.Config;
import org.ros.namespace.GraphName;

/**
 * An example of implementation of talker on topic of custom type.
 */
public class TalkerCustom {

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
		TopicPublishHandler<CustomMsg> publishHandler =
				TopicManager.publishOnTopic(GraphName.of("/foo"), CustomMsg.class);

		// wait until ready
		publishHandler.blockUntilReady();

		// create message
		CustomMsg msg = publishHandler.newMessage();
		for (int i = 0; i < 100; i++) {
			// edit message
			msg.setFoo("hello");
			msg.setBar(i);

			// publish message
			publishHandler.publish(msg);
			System.out.println(String.format("published %s - %d", msg.getFoo(), msg.getBar()));

			// wait a sec
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e) {}
		}

		// close handler
		publishHandler.close();

		// exit
		System.exit(0);
	}

}
