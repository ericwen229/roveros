package com.ericwen229.util;

import lombok.NonNull;
import org.jboss.netty.buffer.ChannelBuffer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Image utilities related to ROS applications.
 */
public class Image {

	/**
	 * Created image from ROS image message.
	 *
	 * @param imageMsg image message
	 * @return image object
	 */
	public static BufferedImage imageMessageToBufferdImage(@NonNull sensor_msgs.Image imageMsg) {
		// TODO: handle different encodings
		String imageEncoding = imageMsg.getEncoding();
		if (!imageEncoding.equalsIgnoreCase("bgr8")) {
			throw new RuntimeException("Unsupported encoding: " + imageEncoding);
		}
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
	 * Encode buffered image to byte array with given encoding.
	 *
	 * @param bufferedImage image
	 * @param encoding encoding
	 * @return byte array of encoded image
	 */
	public static byte[] bufferedImageToByteArray(@NonNull BufferedImage bufferedImage, @NonNull String encoding) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			ImageIO.write(bufferedImage, encoding, outputStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return outputStream.toByteArray();
	}

	/**
	 * Create RGB value from given BGR component values
	 *
	 * @param r red component value
	 * @param g green component value
	 * @param b blue component value
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
