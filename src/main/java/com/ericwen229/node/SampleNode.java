package com.ericwen229.node;

import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

public class SampleNode implements NodeMain {

    public GraphName getDefaultNodeName() {
        return GraphName.of("sample");
    }

    public void onStart(ConnectedNode connectedNode) {
        final Publisher<std_msgs.String> publisher = connectedNode.newPublisher("sampletopic", std_msgs.String._TYPE);
        connectedNode.executeCancellableLoop(new CancellableLoop() {
            private int id = 0;

            @Override
            protected void loop() throws InterruptedException {
                std_msgs.String msg = publisher.newMessage();
                msg.setData(String.format("hello #%d", id++));
                publisher.publish(msg);
                Thread.sleep(1000);
            }
        });
    }

    public void onShutdown(Node node) {
    }

    public void onShutdownComplete(Node node) {
    }

    public void onError(Node node, Throwable throwable) {
    }

}
