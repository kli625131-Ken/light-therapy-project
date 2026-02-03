package com.lontri.lighttherapy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lontri.lighttherapy.util.SocketCmdUtil;

@Service
public class TcpSenderService {

    private static final Logger log = LoggerFactory.getLogger(TcpSenderService.class);

    /**
     * TCP 透传发送
     * payload 已经包含网关信息，这里不做任何拼接
     */
    public void sendMessage(String payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("tcp payload is empty");
        }
        try {
            SocketCmdUtil.send(payload);  // 你已有的异步发送
            log.info("TCP发送数据包: {}", payload);
        } catch (Exception e) {
            log.error("TCP发送失败, payload={}", payload, e);
            throw e;
        }
    }
}
