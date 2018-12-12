package com.ericwen229;

import com.ericwen229.server.ControlServer;
import com.ericwen229.server.VideoServer;

import java.net.InetSocketAddress;

public class ApplicationMain {

    public static void main(String[] args) {
        ControlServer controlServer = new ControlServer(new InetSocketAddress(8886));
        controlServer.start(); // it will start in a new thread

        VideoServer videoServer = new VideoServer(new InetSocketAddress(8887));
        videoServer.start(); // it will start in a new thread
    }

}
