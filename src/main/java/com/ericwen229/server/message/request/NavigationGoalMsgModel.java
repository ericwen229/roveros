package com.ericwen229.server.message.request;

/**
 * Message model of navigation goal used for message deserialization.
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
