package com.ericwen229.server.model;

import lombok.NonNull;

public class CameraImageMsgModel {

	public String type = "camera_image";
	public String encoding = "base64";
	public String image;

	public CameraImageMsgModel(@NonNull String base64ImageStr) {
		this.image = base64ImageStr;
	}

}
