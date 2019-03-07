package com.ericwen229.server.message.request;

/**
 * This class describes the model of pose estimate message, which is used to
 * approximately estimate Turtlebot's pose on the map.
 */
public class PoseEstimateMsgModel extends RequestMsgModel {

	/**
	 * Used by gson to perform dynamic dispatch.
	 */
	public static final String typeFieldValue = "pose_estimate";

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
