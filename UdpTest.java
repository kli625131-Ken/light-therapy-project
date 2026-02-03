import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class UdpTest {
    public static void main(String[] args) {
        String ip = "127.0.0.1";
        int port = 45678;
        String payload = "{\"cmdId\": \"setBrightness\", \"params\": {\"value\": 50}}";
        
        try (DatagramSocket socket = new DatagramSocket()) {
            System.out.println("Sending UDP command to " + ip + ":" + port + ", payload: " + payload);
            byte[] data = payload.getBytes(StandardCharsets.US_ASCII);
            InetAddress addr = InetAddress.getByName(ip.trim());
            DatagramPacket packet = new DatagramPacket(data, data.length, addr, port);
            socket.send(packet);
            System.out.println("UDP command sent successfully!");
        } catch (Exception e) {
            System.out.println("UDP send failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}