import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Main {
    public static void main(String[] args) {

        int port = 10000;
        byte[] data = new byte[1209];

        try {
            DatagramSocket receiverSocket = new DatagramSocket(port);
            DatagramPacket packet = new DatagramPacket(data, data.length);
            System.out.println("Waiting for packets...");
            try{
                while(true){

                    receiverSocket.receive(packet);

                    int c;
                    ByteArrayInputStream bInput = new ByteArrayInputStream(packet.getData());

                    for(int y = 0 ; y < 1; y++ ) {
                        while ((c = bInput.read()) != -1) {
                            System.out.println(c);
                        }
                        bInput.reset();
                    }

                }
            }catch (Exception e){

            }

        } catch (SocketException e) {
            e.printStackTrace();
        }


    }
}
