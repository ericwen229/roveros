package com.ericwen229.util;

import org.jboss.netty.buffer.ChannelBuffer;
import sensor_msgs.Image;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public class ImageUtil {

    public static BufferedImage imageFromMessage(Image imageMsg) {
        // TODO: handle different encodings
        String imageEncoding = imageMsg.getEncoding();
        int imageWidth = imageMsg.getWidth();
        int imageHeight = imageMsg.getHeight();

        // TODO: handle different encodings
        byte isBigEndian = imageMsg.getIsBigendian();

        ChannelBuffer data = imageMsg.getData();
        int arrayOffset = data.arrayOffset();
        byte[] dataArray = data.array();
        dataArray = Arrays.copyOfRange(dataArray, arrayOffset, dataArray.length);

        BufferedImage outputImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < imageWidth; i++) {
            for (int j = 0; j < imageHeight; j++) {
                int offset = (imageWidth * j + i) * 3;
                outputImage.setRGB(i, j, ColorUtil.rgbfromBGR(dataArray[offset], dataArray[offset+1], dataArray[offset+2]));
            }
        }

        return outputImage;
    }

}
