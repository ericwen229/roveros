package com.ericwen229.server.message.request;

/**
 * This class describes the model of navigation goal message, which is used
 * to navigate Turtlebot on the map.
 */
public class NavigationGoalMsgModel extends RequestMsgModel {

	/**
	 * Used by gson to perform dynamic dispatch.
	 */
	public static final String typeFieldValue = "navigation_goal";

	/**
	 * Coordinate X on the map.
	 */
	public double x;

	/**
	 * Coordinate Y on the map.
	 */
	public double y;

	/**
	 * Orientation angle.
	 */
	public double angle;

}
