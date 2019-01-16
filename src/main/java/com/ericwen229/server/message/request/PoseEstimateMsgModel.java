package com.ericwen229.server.message.request;

public class PoseEstimateMsgModel extends RequestMsgModel {

	public static final String typeFieldValue = "pose_estimate";

	public double x;
	public double y;
	public double angle;

}
