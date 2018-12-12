package com.ericwen229.util;

public class ColorUtil {

    public static int rgbfromBGR(byte b, byte g, byte r) {
        int color = 0;

        color |= 0x00ff0000 & ((int)r << 16);
        color |= 0x0000ff00 & ((int)g << 8);
        color |= 0x000000ff & (int)b;

        return color;
    }

}
