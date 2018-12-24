package com.ericwen229.util;

import org.jboss.netty.buffer.ChannelBuffer;

import java.awt.image.BufferedImage;

/**
 * Image utilities related to ROS applications.
 */
public class Image {

	// ========== static methods ==========

	/**
	 * Created image from ROS image message.
	 *
	 * @param imageMsg image message
	 * @return image object
	 */
	public static BufferedImage imageFromMessage(sensor_msgs.Image imageMsg) {
		// TODO: handle different encodings
		String imageEncoding = imageMsg.getEncoding();
		int imageWidth = imageMsg.getWidth();
		int imageHeight = imageMsg.getHeight();

		// TODO: handle different encodings
		byte isBigEndian = imageMsg.getIsBigendian();

		ChannelBuffer data = imageMsg.getData();
		int arrayOffset = data.arrayOffset();
		byte[] dataArray = data.array();

		BufferedImage outputImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		for (int i = 0; i < imageWidth; i++) {
			for (int j = 0; j < imageHeight; j++) {
				int index = (imageWidth * j + i) * 3 + arrayOffset;
				outputImage.setRGB(i, j, rgbFromRGB(dataArray[index + 2], dataArray[index + 1], dataArray[index]));
			}
		}

		return outputImage;
	}

	/**
	 * Create RGB value from given BGR component values
	 *
	 * @param b blue component value
	 * @param g green component value
	 * @param r red component value
	 * @return RGB value
	 */
	public static int rgbFromRGB(byte r, byte g, byte b) {
		int color = 0;

		color |= 0x00ff0000 & ((int) r << 16);
		color |= 0x0000ff00 & ((int) g << 8);
		color |= 0x000000ff & (int) b;

		return color;
	}

}
