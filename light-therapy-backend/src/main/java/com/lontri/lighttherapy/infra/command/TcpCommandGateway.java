package com.lontri.lighttherapy.infra.command;

import com.lontri.lighttherapy.executor.port.CommandGateway;
import com.lontri.lighttherapy.util.SocketCmdUtil; // ← 改成你真实包名

import org.springframework.stereotype.Component;

@Component
public class TcpCommandGateway implements CommandGateway {

	@Override
    public void send(String deviceSnForLog, String payload) {
        if (payload == null || payload.isEmpty()) {
            throw new CommandSendException("TCP", deviceSnForLog, payload, "tcp payload empty", null);
        }
        try {
            SocketCmdUtil.send(payload);
        } catch (Exception e) {
            throw new CommandSendException("TCP", deviceSnForLog, payload, "tcp send invoke failed", e);
        }
    }
}
