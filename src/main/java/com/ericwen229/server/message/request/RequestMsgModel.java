package com.ericwen229.server.message.request;

/**
 * Abstract message model used for message deserialization.
 */
public abstract class RequestMsgModel {

	/**
	 * Used by gson to perform dynamic dispatch.
	 */
	public static final String typeFieldName = "type";

	/**
	 * Used by gson to perform dynamic dispatch.
	 */
	public String type;

}
