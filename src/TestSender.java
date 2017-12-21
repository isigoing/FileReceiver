import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.zip.CRC32;
import java.util.zip.Checksum;


public class TestSender implements Runnable {
    @Override
    public void run() {

        byte[] data = new byte[1200];
//        Arrays.fill(data, (byte) 1);
        data[1199] = 1;

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);
        try {



            Checksum checksum = new CRC32();


            System.out.println(out.size());
            data[0] = 'h';
            data[1] = 'e';
            data[2] = 'l';
            data[3] = 'l';
            data[4] = 'o';
            data[5] = '.';
            data[6] = 't';
            data[7] = 'x';
            data[8] = 't';
            checksum.update(data, 0, data.length);
            System.out.println(checksum.getValue());

            out.write(0);
            out.writeLong(checksum.getValue());
            out.writeInt(9);
            out.write(data);
            data = byteOut.toByteArray();

            System.out.println("data length: " + data.length);
            byte b[] = byteOut.toByteArray();
            int counter = 0;
            for (int i = 0; i < b.length; i++) {
                System.out.println(b[i] + " ");
                counter++;
            }
            System.out.println("counter: " + counter);
            System.out.println("------------------------------------------------------------------\n" +
                    "------------------------------------------------------------------\n" +
                    "------------------------------------------------------------------\n" +
                    "------------------------------------------------------------------\n");

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {

            DatagramSocket clientSocket = new DatagramSocket();
            System.out.println("Ready to send some data");
            InetAddress ia = InetAddress.getLocalHost();
            DatagramPacket packet = new DatagramPacket(data, data.length, ia, 10000);
            clientSocket.send(packet);


            byte[] pkt = new byte[9];
            DatagramSocket receiverSocket = new DatagramSocket(9000);
            receiverSocket.setSoTimeout(10_000);
            DatagramPacket ackpkt = new DatagramPacket(pkt, pkt.length);
            System.out.println("Waiting for ACK...");

            receiverSocket.receive(ackpkt);
            System.out.println("received ack");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
