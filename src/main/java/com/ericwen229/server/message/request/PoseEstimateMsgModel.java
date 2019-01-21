package com.ericwen229.server.message.request;

/**
 * Message model of pose estimate used for message deserialization.
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
