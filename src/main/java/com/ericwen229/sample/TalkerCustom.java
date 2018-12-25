package com.ericwen229.sample;

import com.ericwen229.node.NodeManager;
import com.ericwen229.topic.PublisherHandler;
import com.ericwen229.topic.TopicManager;
import com.ericwen229.util.Config;
import org.ros.namespace.GraphName;

public class TalkerCustom {

	// TODO: move this out of project source
	public static void main(String[] args) {
		// load config
		Config.loadConfig(args[0]);

		// create handler
		PublisherHandler<CustomMsg> handler = TopicManager.createPublisherHandler(GraphName.of("/foo"), CustomMsg.class);

		// add shutdown hook for ctrl-c exit
		Runtime.getRuntime().addShutdownHook(new Thread(handler::close));

		// keep firing new messages
		CustomMsg msg = handler.newMessage();
		for (int i = 0; i < 100; i++) {
			// publish a new message
			msg.setFoo("hello");
			msg.setBar(i);
			System.out.println(String.format("sent %s", msg));
			handler.publish(msg);

			try {
				// wait a second
				Thread.sleep(500);
			}
			catch (InterruptedException e) {}
		}

		// shutdown
		NodeManager.shutdown();
	}

}
