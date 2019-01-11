package com.ericwen229.util;

import lombok.NonNull;
import org.ros.namespace.GraphName;

/**
 * Utils to generate ROS names.
 */
public class Name {

	/**
	 * Generate name for publisher node.
	 *
	 * @param topicName topic name
	 * @return publisher node name generated
	 */
	public static GraphName getPublisherNodeName(@NonNull GraphName topicName) {
		return GraphName.of(String.format("/roveros/publish%s", topicName));
	}

	/**
	 * Generate name for subscriber node.
	 *
	 * @param topicName topic name
	 * @return subscriber node name generated
	 */
	public static GraphName getSubscriberNodeName(@NonNull GraphName topicName) {
		return GraphName.of(String.format("/roveros/subscribe%s", topicName));
	}

}
