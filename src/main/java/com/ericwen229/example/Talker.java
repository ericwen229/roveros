package com.ericwen229.example;

import com.ericwen229.node.NodeManager;
import com.ericwen229.node.TopicPublishHandler;
import com.ericwen229.topic.TopicManager;
import com.ericwen229.util.Config;
import org.ros.namespace.GraphName;

/**
 * An example of talker implementation.
 */
public class Talker {

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
		TopicPublishHandler<std_msgs.String> publishHandler =
				TopicManager.publishOnTopic(GraphName.of("/foo"), std_msgs.String.class);

		// block until ready
		publishHandler.blockUntilReady();

		// create message
		std_msgs.String msg = publishHandler.newMessage();
		for (int i = 0; i < 20; i++) {
			// edit message
			msg.setData(Integer.toString(i));

			// publish message
			publishHandler.publish(msg);
			System.out.println(String.format("published %s", msg.getData()));

			// wait a sec
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
