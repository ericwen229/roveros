package com.ericwen229;

import com.ericwen229.server.ControlServer;
import com.ericwen229.server.VideoServer;
import com.ericwen229.util.Config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;

public class ApplicationMain {

	public static void main(String[] args) {
		try {
			Config.loadConfig(new FileInputStream(args[0]));
		} catch (FileNotFoundException e) {
			// TODO
		}

		ControlServer controlServer = new ControlServer(
				new InetSocketAddress(
						Config.getIntProperty("controlserverport")));
		controlServer.start(); // it will start in a new thread

		VideoServer videoServer = new VideoServer(new InetSocketAddress(Config.getIntProperty("videoserverport")));
		videoServer.start(); // it will start in a new thread
	}

}
