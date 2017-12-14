import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class Main {
    public static void main(String[] args) {

        int port = 10000;
        byte[] data = new byte[1209];

        try {
            DatagramSocket receiverSocket = new DatagramSocket(port);
            DatagramPacket packet = new DatagramPacket(data, data.length);
            System.out.println("Waiting for packets...");
            try {
                while (true) {
                    System.out.println("Start");
                    receiverSocket.receive(packet);

                    DataInputStream in = new DataInputStream(new ByteArrayInputStream(packet.getData()));
                    int c;

                    int ack = in.read();
                    System.out.println("ack " + ack);

                    byte[] checksum = new byte[8];

                    for (int i = 0; i < checksum.length; i++) {
                        checksum[i] = in.readByte();
                        System.out.println("checksum " + checksum[i]);
                    }

                    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
                    buffer.put(checksum);
                    buffer.flip();
                    System.out.println("buffer " + buffer.getLong());


                    for (int y = 0; y < 1; y++) {
                        while ((c = in.read()) != -1) {
                            System.out.println("data " + c);
                        }
                        in.reset();
                    }

                    System.out.println("End");
                }
            } catch (Exception e) {

            }

        } catch (SocketException e) {
            e.printStackTrace();
        }


    }
}
