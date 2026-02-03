import com.lontri.lighttherapy.entity.Device;
import com.lontri.lighttherapy.enums.SendMode;
import com.lontri.lighttherapy.infra.command.sender.UdpCommandSender;

public class UdpCommandTest {
    public static void main(String[] args) {
        // Create test device configuration
        Device device = new Device();
        device.setDeviceSn("LTR-GW-1000000003-00001");
        device.setSendMode(SendMode.UDP);
        device.setUdpIp("127.0.0.1");
        device.setUdpPort(45678);

        // Create device command
        String command = "{\"cmdId\": \"setBrightness\", \"params\": {\"value\": 50}}";

        // Use UdpCommandSender to send command
        UdpCommandSender udpSender = new UdpCommandSender();
        boolean success = udpSender.send(device, command);
        
        System.out.println("UDP command send result: " + (success ? "success" : "failure"));
    }
}