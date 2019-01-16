package com.ericwen229.server.message.response;

import lombok.NonNull;

public class CameraImageMsgModel {

	private final String type = "camera_image";
	private final String encoding = "base64";
	private final String image;

	public CameraImageMsgModel(@NonNull String base64ImageStr) {
		this.image = base64ImageStr;
	}

}
