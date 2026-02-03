package com.lontri.lighttherapy.infra.command;

public class CommandSendException extends RuntimeException {
    private final String channel;  // UDP / TCP
    private final String deviceSn;
    private final String payload;

    public CommandSendException(String channel, String deviceSn, String payload, String message, Throwable cause) {
        super(message, cause);
        this.channel = channel;
        this.deviceSn = deviceSn;
        this.payload = payload;
    }

    public String getChannel() { return channel; }
    public String getDeviceSn() { return deviceSn; }
    public String getPayload() { return payload; }
}
