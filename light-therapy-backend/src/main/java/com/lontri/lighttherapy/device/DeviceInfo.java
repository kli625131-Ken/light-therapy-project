package com.lontri.lighttherapy.device;

import com.lontri.lighttherapy.enums.SendMode;

public class DeviceInfo {

    private String deviceSn;

    /** 路由用：网关ID（TCP 必须带） */
    private String gatewayId;

    /** UDP 目标地址 */
    private String udpIp;
    private Integer udpPort;

    /** UDP / TCP */
    private SendMode sendMode;

    public String getDeviceSn() { return deviceSn; }
    public void setDeviceSn(String deviceSn) { this.deviceSn = deviceSn; }

    public String getGatewayId() { return gatewayId; }
    public void setGatewayId(String gatewayId) { this.gatewayId = gatewayId; }

    public String getUdpIp() { return udpIp; }
    public void setUdpIp(String udpIp) { this.udpIp = udpIp; }

    public Integer getUdpPort() { return udpPort; }
    public void setUdpPort(Integer udpPort) { this.udpPort = udpPort; }
	public SendMode getSendMode() {
		return sendMode;
	}
	public void setSendMode(SendMode sendMode) {
		this.sendMode = sendMode;
	}
	

    
}
