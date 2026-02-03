package com.lontri.lighttherapy.service;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

@Service
public class UdpSenderService {

    private static final Logger log = Logger.getLogger(UdpSenderService.class);

    public void sendMessage(String message, String ip, int port) throws Exception {
        DatagramSocket socket = new DatagramSocket();
        InetAddress targetAddress = InetAddress.getByName(ip);

        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, targetAddress, port);

        socket.send(packet);
        log.info("UDP发送数据包: " + message + " to " + ip + ":" + port);
        socket.close();
    }

    // 兼容你现在调用签名（Integer）
    public void sendMessage(String cmd, String udpIp, Integer udpPort) throws Exception {
        if (udpPort == null) throw new IllegalArgumentException("udpPort is null");
        sendMessage(cmd, udpIp, udpPort.intValue());
    }
}
