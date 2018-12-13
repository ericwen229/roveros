package com.ericwen229.util;

import org.jboss.netty.buffer.ChannelBuffer;

import java.awt.image.BufferedImage;

public class Image {

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
                outputImage.setRGB(i, j, rgbfromBGR(dataArray[index], dataArray[index+1], dataArray[index+2]));
            }
        }

        return outputImage;
    }

    public static int rgbfromBGR(byte b, byte g, byte r) {
        int color = 0;

        color |= 0x00ff0000 & ((int)r << 16);
        color |= 0x0000ff00 & ((int)g << 8);
        color |= 0x000000ff & (int)b;

        return color;
    }

}
