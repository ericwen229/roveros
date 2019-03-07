package com.ericwen229;

import com.ericwen229.node.RoverOSNode;
import com.ericwen229.server.NavigationServer;
import com.ericwen229.util.PropertiesChecked;
import org.ros.namespace.GraphName;

import java.net.InetSocketAddress;
import java.net.URI;

/**
 * This class is the main entry of RoverOS application.
 */
public class ApplicationMain {

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: java -jar <jar file> <properties file>");
			System.exit(-1);
		}

		// read configuration
		PropertiesChecked properties = new PropertiesChecked(args[0]);
		String host = properties.getPropertyChecked("host");
		URI masterURI = URI.create(properties.getPropertyChecked("masterURI"));

		// create and run ROS node
		RoverOSNode node = RoverOSNode.newPublicNode(GraphName.of("roveros"), host, masterURI);
		node.run();

		// create and start navigation server
		int navigationServerPort = Integer.parseInt(properties.getPropertyChecked("navigationServerPort"));
		NavigationServer navigationServer = new NavigationServer(node, new InetSocketAddress(navigationServerPort));
		navigationServer.start();
	}

}
