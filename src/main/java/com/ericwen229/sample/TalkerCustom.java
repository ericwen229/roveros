package com.ericwen229.sample;

import com.ericwen229.node.PublisherNodeHandler;
import com.ericwen229.topic.TopicManager;
import com.ericwen229.util.Config;
import org.ros.namespace.GraphName;

public class TalkerCustom {

	public static void main(String[] args) {
		Config.loadConfig(args[0]);

		PublisherNodeHandler<CustomMsg> handler =
				TopicManager.publishOnTopic(GraphName.of("/foo"), CustomMsg.class);
		handler.blockUntilReady();

		CustomMsg msg = handler.newMessage();
		for (int i = 0; i < 100; i++) {
			msg.setFoo("hello");
			msg.setBar(i);
			System.out.println(String.format("published %s - %d", msg.getFoo(), msg.getBar()));
			handler.publish(msg);

			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e) {}
		}

		System.exit(0);
	}

}
