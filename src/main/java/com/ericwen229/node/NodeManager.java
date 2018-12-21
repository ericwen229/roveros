package com.ericwen229.node;

import com.ericwen229.util.Config;
import lombok.NonNull;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Node management, including node execution, shutdown and node configuration management.
 */
public class NodeManager {

	// ========== static members ==========

	/**
	 * Node executor.
	 */
	private static final NodeMainExecutor defaultNodeExecutor = DefaultNodeMainExecutor.newDefault();

	/**
	 * Node configuration
	 */
	private static NodeConfiguration nodeConfig = null;



	// ========== static methods ==========

	/**
	 * Create node configuration from configuration file. The configuration will be preserved after
	 * this method is invoked for the first time.
	 *
	 * @return node configuration
	 */
	private static NodeConfiguration getNodeConfiguration() {
		if (nodeConfig == null) {
			try {
				URI masterURI = new URI(Config.getProperty("masteruri"));
				String host = Config.getProperty("host");
				nodeConfig = NodeConfiguration.newPublic(host, masterURI);
			} catch (URISyntaxException e) {
				throw new RuntimeException();
			}
		}
		return nodeConfig;
	}

	/**
	 * Execute given node.
	 *
	 * @param node node to execute
	 */
	public static void executeNode(@NonNull NodeMain node) {
		defaultNodeExecutor.execute(node, getNodeConfiguration());
	}

	/**
	 * Shutdown given node.
	 *
	 * @param node node to shutdown
	 */
	public static void shutdownNode(@NonNull NodeMain node) {
		defaultNodeExecutor.shutdownNodeMain(node);
	}

	/**
	 * Shutdown all nodes and thread pool. This should only be invoked on program termination.
	 */
	public static void shutdown() {
		defaultNodeExecutor.shutdown(); // shutdown all nodes
		defaultNodeExecutor.getScheduledExecutorService().shutdown(); // shutdown thread pool
	}

}
