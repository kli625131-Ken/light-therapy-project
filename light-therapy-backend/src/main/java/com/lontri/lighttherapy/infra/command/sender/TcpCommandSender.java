package com.lontri.lighttherapy.infra.command.sender;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import com.lontri.lighttherapy.infra.command.CommandSendException;

@Component
public class TcpCommandSender {

    public void send(String deviceSnForLog, String ip, Integer port, String payload) {
        if (ip == null || ip.trim().isEmpty()) {
            throw new CommandSendException("TCP", deviceSnForLog, payload, "tcp_server_ip is empty", null);
        }
        if (port == null || port <= 0) {
            throw new CommandSendException("TCP", deviceSnForLog, payload, "tcp_server_port invalid", null);
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip.trim(), port), 2000);
            socket.setSoTimeout(2000);

            OutputStream os = socket.getOutputStream();
            os.write(payload.getBytes(StandardCharsets.US_ASCII));
            os.flush();
        } catch (Exception e) {
            throw new CommandSendException("TCP", deviceSnForLog, payload,
                    "tcp send failed to " + ip + ":" + port, e);
        }
    }
}
