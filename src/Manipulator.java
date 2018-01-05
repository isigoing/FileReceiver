import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

public class Manipulator {
    private DatagramSocket socket;
    private int lossCounter = 0;
    private int dupCounter = 0;
    private int errCounter = 0;
    private DatagramPacket prev;
    private DatagramPacket returnPacket;

    Manipulator(DatagramSocket socket) {
        this.socket = socket;
    }

    public DatagramPacket manipulate(DatagramPacket packet) {
        double rand = Math.random();
        try {
            socket.receive(packet);
            returnPacket = packet;

//            if (rand < 0.1) {
//                // verwerfen
//                lossCounter++;
//                returnPacket = null;
//            }

//            if (rand > 0.1 && rand < 0.15) {
//                // duplizieren
//                dupCounter++;
//                returnPacket = prev;
//
//            }

            if (rand > 0.15 && rand < 0.2) {
                // bitfehler
                errCounter++;
                packet.getData()[1000] = (byte) ~packet.getData()[1000]; //flipped das bit
                returnPacket = packet;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        prev = returnPacket;
        return returnPacket;
    }

    public void printData() {
        System.out.println("#################################");
        System.out.println("Verworfen: " + lossCounter);
        System.out.println("Dupliziert: " + dupCounter);
        System.out.println("Bitfehler: " + errCounter);
        System.out.println("#################################");
    }
}
