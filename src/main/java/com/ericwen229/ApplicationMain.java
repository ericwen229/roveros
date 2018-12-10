package com.ericwen229;

import com.ericwen229.server.ControlServer;

import java.net.InetSocketAddress;

public class ApplicationMain {

    public static void main(String[] args) {
        ControlServer controlServer = new ControlServer(new InetSocketAddress("localhost", 8886));
        controlServer.run();
    }

}
