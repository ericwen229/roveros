package com.ericwen229.node;

import com.ericwen229.util.Config;
import lombok.NonNull;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;

import java.net.URI;
import java.net.URISyntaxException;

public class NodeManager {

	private static final NodeMainExecutor defaultNodeExecutor = DefaultNodeMainExecutor.newDefault();
	private static NodeConfiguration nodeConfig = null;

	private static NodeConfiguration getConfiguration() {
		if (nodeConfig == null) {
			try {
				URI masterURI = new URI(Config.getStringProperty("masteruri"));
				String host = Config.getStringProperty("host");
				nodeConfig = NodeConfiguration.newPublic(host, masterURI);
			} catch (URISyntaxException e) {
				throw new RuntimeException();
			}
		}
		return nodeConfig;
	}

	public static void executeNode(@NonNull NodeMain node) {
		defaultNodeExecutor.execute(node, getConfiguration());
	}

}
