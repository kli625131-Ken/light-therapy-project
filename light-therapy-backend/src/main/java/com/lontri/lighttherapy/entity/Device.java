package com.lontri.lighttherapy.entity;

import javax.persistence.*;

import com.lontri.lighttherapy.enums.DeviceType;
import com.lontri.lighttherapy.enums.SendMode;

@Entity
@Table(
    name = "device",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_device_gateway_sn",
        columnNames = {"gateway_id", "device_sn"}
    )
)
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ 24位字符串
    @Column(name="gateway_id", length = 24)
    private String gatewayId;

    @Column(name="device_sn", length = 64, nullable = false)
    private String deviceSn;

    @Column(name="device_name", length = 128)
    private String deviceName;

    // ✅ ENUM('UDP','TCP')
    @Enumerated(EnumType.STRING)
    @Column(name="send_mode", length = 16, nullable = false)
    private SendMode sendMode;

    // ✅ ENUM('LONTRI','DALI','485')
    @Convert(converter = DeviceTypeConverter.class)
    @Column(name="device_type", length = 16, nullable = false)
    private DeviceType deviceType = DeviceType.LONTRI;

    @Column(name="udp_ip", length = 64)
    private String udpIp;

    @Column(name="udp_port")
    private Integer udpPort;

    @Column(name="tcp_server_ip", length = 64)
    private String tcpServerIp;

    @Column(name="tcp_server_port")
    private Integer tcpServerPort;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getGatewayId() {
		return gatewayId;
	}

	public void setGatewayId(String gatewayId) {
		this.gatewayId = gatewayId;
	}

	public String getDeviceSn() {
		return deviceSn;
	}

	public void setDeviceSn(String deviceSn) {
		this.deviceSn = deviceSn;
	}

	public SendMode getSendMode() {
		return sendMode;
	}

	public void setSendMode(SendMode sendMode) {
		this.sendMode = sendMode;
	}

	public DeviceType getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(DeviceType deviceType) {
		this.deviceType = deviceType;
	}

	public String getUdpIp() {
		return udpIp;
	}

	public void setUdpIp(String udpIp) {
		this.udpIp = udpIp;
	}

	public Integer getUdpPort() {
		return udpPort;
	}

	public void setUdpPort(Integer udpPort) {
		this.udpPort = udpPort;
	}

	public String getTcpServerIp() {
		return tcpServerIp;
	}

	public void setTcpServerIp(String tcpServerIp) {
		this.tcpServerIp = tcpServerIp;
	}

	public Integer getTcpServerPort() {
		return tcpServerPort;
	}

	public void setTcpServerPort(Integer tcpServerPort) {
		this.tcpServerPort = tcpServerPort;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

    
}
