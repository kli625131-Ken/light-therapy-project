import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class udp_listener {
    public static void main(String[] args) {
        try {
            // Create UDP socket, listen on port 45678
            DatagramSocket socket = new DatagramSocket(null);
            // Set SO_REUSEADDR option to allow reuse of local addresses
            socket.setReuseAddress(true);
            socket.bind(new java.net.InetSocketAddress(45678));
            System.out.println("UDP listener started, listening on port: 45678");
            System.out.println("Waiting for UDP data...");
            
            byte[] buffer = new byte[1024];
            
            while (true) {
                // Create packet for receiving data
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                
                // Receive UDP packet
                socket.receive(packet);
                
                // Parse received data
                String receivedData = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.US_ASCII);
                String senderAddress = packet.getAddress().getHostAddress();
                int senderPort = packet.getPort();
                
                // Print received data
                System.out.println("========================================");
                System.out.println("Received UDP data:");
                System.out.println("Sender: " + senderAddress + ":" + senderPort);
                System.out.println("Data: " + receivedData);
                System.out.println("Length: " + receivedData.length() + " bytes");
                System.out.println("========================================");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}