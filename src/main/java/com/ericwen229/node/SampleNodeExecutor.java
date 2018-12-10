package com.ericwen229.node;

import org.ros.exception.RosRuntimeException;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.util.Scanner;

public class SampleNodeExecutor {
    public static void main(String[] args) {
        // run control node
        NodeConfiguration config = NodeConfiguration.newPrivate();
        NodeMainExecutor executor = DefaultNodeMainExecutor.newDefault();
        RoverControllerNode node = new RoverControllerNode();
        executor.execute(node, config); // node won't be ready right away

        // sample code used for setting control state
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            double linear = scanner.nextDouble();
            double angular = scanner.nextDouble();

            try {
                node.publish(linear, angular);
            }
            catch (RosRuntimeException e) {
                System.out.println("publish failed");
            }
        }
    }
}
