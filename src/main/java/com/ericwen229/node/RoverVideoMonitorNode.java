package com.ericwen229.node;

import com.ericwen229.util.pattern.Observer;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;
import sensor_msgs.Image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoverVideoMonitorNode implements NodeMain {

	// ========== members ==========

	private Subscriber<Image> subscriber = null;
	private final List<Observer<Image>> observers = Collections.synchronizedList(new ArrayList<>());

	// ========== interface required methods ==========

	public GraphName getDefaultNodeName() {
		return GraphName.of("videomonitor");
	}

	public void onStart(ConnectedNode connectedNode) {
		// TODO: manage name
		subscriber = connectedNode.newSubscriber("/camera/rgb/image_raw", Image._TYPE);
		subscriber.addMessageListener(imageMsg -> {
			notifyObservers(imageMsg);
		});
	}

	public void onShutdown(Node node) {
	}

	public void onShutdownComplete(Node node) {
	}

	public void onError(Node node, Throwable throwable) {
	}

	// ========== observer pattern ==========

	private void notifyObservers(Image imageMsg) {
		for (Observer<Image> o : observers) {
			o.notify(imageMsg);
		}
	}

	public void addObserver(Observer<Image> observer) {
		observers.add(observer);
	}

	public void removeObserver(Observer<Image> observer) {
		observers.remove(observer);
	}

}
