package com.lontri.lighttherapy.infra.command.sender;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.lontri.lighttherapy.infra.command.CommandSendException;

@Component
public class UdpCommandSender {
    private static final Logger log = LoggerFactory.getLogger(UdpCommandSender.class);

    public void send(String deviceSnForLog, String ip, Integer port, String payload) {
        log.info("Attempting to send UDP command to device {}: {}:{}, payload={}", deviceSnForLog, ip, port, payload);
        
        if (ip == null || ip.trim().isEmpty()) {
            log.error("UDP send failed: udp_ip is empty for device {}", deviceSnForLog);
            throw new CommandSendException("UDP", deviceSnForLog, payload, "udp_ip is empty", null);
        }
        if (port == null || port <= 0) {
            log.error("UDP send failed: udp_port invalid for device {}, port={}", deviceSnForLog, port);
            throw new CommandSendException("UDP", deviceSnForLog, payload, "udp_port invalid", null);
        }

        try (DatagramSocket socket = new DatagramSocket()) {
            log.info("Creating UDP socket for device {}", deviceSnForLog);
            byte[] data = payload.getBytes(StandardCharsets.US_ASCII);
            log.info("Converted payload to bytes, length: {}", data.length);
            InetAddress addr = InetAddress.getByName(ip.trim());
            log.info("Resolved IP address: {}", addr);
            DatagramPacket packet = new DatagramPacket(data, data.length, addr, port);
            log.info("Created datagram packet: {}", packet);
            socket.send(packet);
            log.info("UDP command sent successfully to device {}", deviceSnForLog);
        } catch (Exception e) {
            log.error("UDP send failed to {}:{}, device={}, payload={}", ip, port, deviceSnForLog, payload, e);
            throw new CommandSendException("UDP", deviceSnForLog, payload,
                    "udp send failed to " + ip + ":" + port, e);
        }
    }
}
