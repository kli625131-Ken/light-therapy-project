package com.lontri.lighttherapy.infra.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.lontri.lighttherapy.entity.Device;
import com.lontri.lighttherapy.enums.SendMode;
import com.lontri.lighttherapy.executor.port.CommandGateway;
import com.lontri.lighttherapy.infra.command.sender.TcpCommandSender;
import com.lontri.lighttherapy.infra.command.sender.UdpCommandSender;
import com.lontri.lighttherapy.repository.DeviceRepository;

@Component
public class CommandGatewayRouter implements CommandGateway {

    private static final Logger log = LoggerFactory.getLogger(CommandGatewayRouter.class);
    private final DeviceRepository deviceRepo;
    private final TcpCommandSender tcpSender;
    private final UdpCommandSender udpSender;

    public CommandGatewayRouter(DeviceRepository deviceRepo,
                                TcpCommandSender tcpSender,
                                UdpCommandSender udpSender) {
        this.deviceRepo = deviceRepo;
        this.tcpSender = tcpSender;
        this.udpSender = udpSender;
    }

    @Override
    public void send(String deviceSnForLog, String payload) {
        log.info("Starting to send command to device: {}", deviceSnForLog);
        log.info("Raw payload: {}", payload);

        if (payload == null || payload.isEmpty()) {
            log.error("Command send failed: payload is empty for device {}", deviceSnForLog);
            throw new CommandSendException("ROUTER", deviceSnForLog, payload, "payload empty", null);
        }

        Device d = deviceRepo.findByDeviceSn(deviceSnForLog)
                .orElseThrow(() -> {
                    log.error("Command send failed: device not found for SN: {}", deviceSnForLog);
                    return new CommandSendException("ROUTER", deviceSnForLog, payload, "device not found", null);
                });

        log.info("Found device: {}", d.getDeviceSn());
        log.info("Device type: {}", d.getDeviceType());
        log.info("Send mode: {}", d.getSendMode());
        log.info("Gateway ID: {}", d.getGatewayId());
        log.info("UDP IP: {}", d.getUdpIp());
        log.info("UDP Port: {}", d.getUdpPort());
        log.info("TCP Server IP: {}", d.getTcpServerIp());
        log.info("TCP Server Port: {}", d.getTcpServerPort());

        SendMode mode = (d.getSendMode() == null) ? SendMode.TCP : d.getSendMode();
        log.info("Selected send mode: {}", mode);

        // 先构建不包含长度字段的命令内容
        String commandContent = "02" + d.getGatewayId() + deviceSnForLog + payload.trim();
        // 计算命令总长度（包括固定前缀"55"和长度字段本身）
        // 总长度 = 固定前缀(2) + 长度字段(2) + 命令内容长度
        int totalLength = 2 + 2 + commandContent.length();
        // 转换为十六进制，每两个字符代表一个字节
        String lengthHex = String.format("%02X", totalLength / 2);
        // 构建最终命令
        String temppayload = "55" + lengthHex + commandContent;
        log.info("Final payload: {}", temppayload);

        if (mode == SendMode.UDP) {
            log.info("Sending UDP command to device: {}", deviceSnForLog);
            log.info("UDP target: {}:{}", d.getUdpIp(), d.getUdpPort());
            udpSender.send(deviceSnForLog, d.getUdpIp(), d.getUdpPort(), temppayload);
            log.info("UDP command sent successfully to device: {}", deviceSnForLog);
        } else {
            log.info("Sending TCP command to device: {}", deviceSnForLog);
            log.info("TCP target: {}:{}", d.getTcpServerIp(), d.getTcpServerPort());
            temppayload =  "OP$Transmit$" + temppayload + "$" + d.getGatewayId();
            log.info("TCP final payload: {}", temppayload);
            tcpSender.send(deviceSnForLog, d.getTcpServerIp(), d.getTcpServerPort(), temppayload);
            log.info("TCP command sent successfully to device: {}", deviceSnForLog);
        }
    }
}
