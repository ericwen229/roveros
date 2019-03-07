package com.ericwen229.server.message.request;

public class ControlMsgModel extends RequestMsgModel {

    /**
     * Used by gson to perform dynamic dispatch.
     */
    public static final String typeFieldValue = "control";

    /**
     * Linear speed.
     */
    public double linear;

    /**
     * Angular speed.
     */
    public double angular;

}
