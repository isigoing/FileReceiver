import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;


public class TestSender {
    public static void main(String[] args) {
        byte[] data = new byte[1200];
        Arrays.fill(data,(byte)1);
        data[1199] = 0;

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);
        try {
            out.write(0);


            Checksum checksum = new CRC32();
            checksum.update(data,0,data.length);
            System.out.println(checksum.getValue());

            out.writeLong(checksum.getValue());

            out.write(data);
            System.out.println(out.size());

            data = byteOut.toByteArray();

            System.out.println(data.length);
//            byte b[] = bOut.toByteArray();
//            for(int i = 0; i < b.length; i++){
//                System.out.println((char)b[i] + " ");
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {

            DatagramSocket clientSocket = new DatagramSocket();
            System.out.println("Ready to send some data");
            InetAddress ia = InetAddress.getLocalHost();
            DatagramPacket packet = new DatagramPacket(data, data.length, ia, 10000);
            clientSocket.send(packet);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
