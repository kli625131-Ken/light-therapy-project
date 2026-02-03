package com.lontri.lighttherapy.infra.command;

import com.lontri.lighttherapy.device.DeviceInfo;
import com.lontri.lighttherapy.service.UdpSenderService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UdpCommandGateway {

    private final UdpSenderService udpSenderService;

    public UdpCommandGateway(UdpSenderService udpSenderService) {
        this.udpSenderService = udpSenderService;
    }

    public void send(DeviceInfo info, List<String> cmds) {
        if (info.getUdpIp() == null || info.getUdpPort() == null) {
            throw new CommandSendException("UDP", info.getDeviceSn(), null,
                    "udp target missing: ip/port null", null);
        }

        for (String cmd : cmds) {
            try {
                udpSenderService.sendMessage(cmd, info.getUdpIp(), info.getUdpPort());
            } catch (Exception e) {
                throw new CommandSendException("UDP", info.getDeviceSn(), cmd,
                        "udp send failed to " + info.getUdpIp() + ":" + info.getUdpPort(), e);
            }
        }
    }
    public void send(DeviceInfo info, String cmd) {
        if (info.getUdpIp() == null || info.getUdpPort() == null) {
            throw new CommandSendException("UDP", info.getDeviceSn(), null,
                    "udp target missing: ip/port null", null);
        }

        try {
            udpSenderService.sendMessage(cmd, info.getUdpIp(), info.getUdpPort());
        } catch (Exception e) {
            throw new CommandSendException("UDP", info.getDeviceSn(), cmd,
                    "udp send failed to " + info.getUdpIp() + ":" + info.getUdpPort(), e);
        }
    }
}
