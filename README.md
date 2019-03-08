# RoverOS

## What is it

RoverOS is a application implemented in Java, with which
one can navigate a Turtlebot in a given map with the help
of a client. Basically it acts as an agent between
clients and the ROS system, forwarding messages between
clients and the ROS system via ROS topics.

## How it works

```
                ROS System
(hardware drivers, SLAM, navigation, etc.)
                    ^
                    |
                    | ROS Communication Mechanism
                    |
                    v
+-----------------------------------------+---+
|                                         |   |
|  ROS node (implemented using rosjava)   |   |
|                                         |   |
+-----------------------------------------+ R |
|                                         | o |
|         representation handling         | v |
|  (encoding, message translation, etc.)  | e |
|                                         | r |
+-----------------------------------------+ O |
|                                         | S |
|           WebSocket Servers             |   |
|                                         |   |
+-----------------------------------------+---+
                    ^
                    |
                    | WebSocket
                    |
                    v
                  Client
```

## How to use it

First, make sure the ROS system is up and running. RoverOS
publishes on the following topics:

* `/cmd_vel_mux/input/teleop` (telecontrol)
* `/initialpose` (estimating initial pose)
* `/move_base_simple/goal` (setting navigation goal)

and subscribes to the following topics:

* `/map_metadata` (retrieving map metadata)
* `/amcl_pose` (retrieving real-time pose estimation)
* `/camera/rgb/image_color` (monitoring through camera)

After that, start RoverOS:

```
java -jar <roveros .jar artifact> <.properties file>
```

Notice that RoverOS expects the path of `.properties` file
as argument. The `.properties` file contains all essential
configurations and is **not** optional. The properties
required are:

* `host`: host address
* `masterURI`: URI of master
* `navigationServerPort`: port of navigation server
* `videoServerPort`: port of video server
* `controlServerPort`: port of control server

The `host` field can be quite confusing. Keep in mind that
**it is used by other ROS nodes to reach the ROS node
of RoverOS**. Therefore the other parts of ROS system
should be able to reach the host serving RoverOS using
the `host` field (either an IP address or a host name).
One common mistake is to give it the value of `127.0.0.1`
while some parts of ROS system is on another host.
If the whole ROS system as long as RoverOS are running
on the same host, `127.0.0.1` would be fine.

## Protocol

### Control server

The server starts at `<ip>:controlServerPort`. Once there's
at least one client connected, the server fires ROS messages
at a constant rate to control the robot. The client can send
messages to modify the speed. Notice that the client's messages
simply change the values RoverOS send to ROS system. The rate
at which RoverOS send ROS messages to ROS system is constant
and is not affected by how fast client sends messages.

#### Messages from client to server

##### Control message

* format:
```
{
  type: "control",
  linear: <double>,
  angular: <double>,
}
```
* fields:
  * type: constant value `"control"` used for dispatching
  * linear: floating point number as linear speed
  * angular: floating point number as angular speed
* Description:
  * Set linear speed and angular speed. A positive linear
  speed makes the robot go forward and a negative one makes
  the robot go backward. A positive angular speed makes the
  robot turn left and a negative one makes the robot turn
  right.
  * The absolute value of linear and angular speed is supposed
  to be less than 1.

### Navigation server

The server starts at `<ip>:navigationServerPort`.

#### Messages from client to server

##### Pose estimate message

* format:
```
{
  type: "pose_estimate",
  x: <double>,
  y: <double>,
  angle: <double>,
}
```
* fields:
  * type: constant value `"pose_estimate"` used for dispatching
  * x: relative x coordinate (0.0 to 1.0) to left of the map
  * y: relative y coordinate (0.0 to 1.0) to right of the map
  * angle: direction to which robot is facing
* Description:
  * Estimate robot's current pose on the map, used by navigation
  modules. One should make estimation as close to the real pose
  as possible.
  * The value of angle is supposed to be between 0.0 and 1.0.
  It corresponds to direction as follows:
```
       0.25
        ^
        |
0.5 <-- * --> 0.0/1.0
        |
        v
       0.75
``` 

##### Navigation goal message

* format:
```
{
  type: "navigation_goal",
  x: <double>,
  y: <double>,
  angle: <double>,
}
```
* fields:
  * type: constant value `"navigation_goal"` used for dispatching
  * x: relative x coordinate (0.0 to 1.0) to left of the map
  * y: relative y coordinate (0.0 to 1.0) to right of the map
  * angle: direction to which robot is facing
* Description:
  * Give robot a goal towards which it will navigate to.
  * The robot will always follow the latest goal.
  * The robot will keep trying for an impossible goal until it bumps
  into an obstacle for long enough to give up.

#### Messages from server to client

##### Pose message

* format:
```
{
  x: <double>,
  y: <double>,
  angle: <double>,
}
```
* fields:
  * x: relative x coordinate (0.0 to 1.0) to left of the map
  * y: relative y coordinate (0.0 to 1.0) to right of the map
  * angle: direction to which robot is facing
* Description:
  * Estimated robot's pose on the map.

### Video server

The server starts at `<ip>:videoServerPort`.

#### Messages from server to client

##### Video message

* format:
```
{
  base64EncodedImageStr: <string>,
}
```
* fields:
  * base64EncodedImageStr: base64 encoded string representing
  an image
* Description:
  * Latest image captured by robot's camera.
  * The image's encoding is JPEG.

## TODOs

* Implement a client (like rviz).
* Add additional functionality like map management, task management, etc.
* Implement adapter for more robot models.
* Perfect the protocol and documentation.