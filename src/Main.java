import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

public class Main {
    public static void main(String[] args) {
        new Thread(new FsmFileReceiver()).start();
        new Thread(new TestSender()).start();
    }
}
