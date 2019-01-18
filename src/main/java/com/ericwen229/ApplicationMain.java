package com.ericwen229;

import com.ericwen229.node.NodeManager;
import com.ericwen229.server.ControlServer;
import com.ericwen229.server.NavigationServer;
import com.ericwen229.server.VideoServer;
import com.ericwen229.util.Config;

import java.net.InetSocketAddress;

public class ApplicationMain {

	public static void main(String[] args) {
		if (args.length != 1) {
			throw new RuntimeException(String.format("Incorrect argument number. %d received but 1 expected.", args.length
			));
		}

		Config.loadConfig(args[0]);
		NodeManager.config(
				Config.getPropertyAsString("host"),
				Config.getPropertyAsString("masterURI"));

		NavigationServer navigationServer = new NavigationServer(new InetSocketAddress(Config.getPropertyAsInt("navserverport")));
		navigationServer.start();

		VideoServer videoServer = new VideoServer(new InetSocketAddress(Config.getPropertyAsInt("videoserverport")));
		videoServer.start();
	}

}
