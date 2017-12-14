import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

public class FsmFileReceiver implements Runnable{
    @Override
    public void run() {
        int ack = 0;
        long checksum = 0;


        int port = 10_000;
        byte[] pkt = new byte[1209];
        byte[] data = new byte[1200];


        try {
            DatagramSocket receiverSocket = new DatagramSocket(port);
            receiverSocket.setSoTimeout(10_000);
            DatagramPacket packet = new DatagramPacket(pkt, pkt.length);
            System.out.println("Waiting for packets...");
            try {
                while (true) {
                    receiverSocket.receive(packet);
                    extractPkt(data, packet);

                    if(currentState == State.WAIT_FOR_ZERO){
                        System.out.println("balbaldldsafhl");
                    }

                }
            }
            catch (SocketTimeoutException e) {
                System.out.println("Timeout Exception");
            }catch (Exception e) {
                System.out.println("Good Morning Exception");
            }

        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
    private static void extractPkt(byte[] data, DatagramPacket packet) throws IOException {
        int ack;
        long checksum;
        System.out.println("Start");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(packet.getData()));

        ack = in.read();
        System.out.println("ack " + ack);

        byte[] check = new byte[8];

        for (int i = 0; i < check.length; i++) {
            check[i] = in.readByte();
            System.out.println("checksum " + check[i]);
        }

        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(check);
        buffer.flip();
        checksum = buffer.getLong();
        System.out.println("checksum " + checksum);

        System.out.println("data length " + data.length);

        for (int i = 0; i < data.length; i++){
            data[i] = in.readByte();
            System.out.println("data " + data[i]);
        }
        System.out.println("End");
    }

    // all states for this FSM
    enum State {
        WAIT_FOR_ZERO, WAIT_FOR_ONE
    }

    // all messages/conditions which can occur
    enum Msg {
        CORRECT_PACKET_ONE, CORRECT_PACKET_ZERO,
    }

    private State currentState;
    private Transition[][] transition;

    public FsmFileReceiver() {
        currentState = State.WAIT_FOR_ZERO;
        transition = new Transition[State.values().length][Msg.values().length];
        transition[State.WAIT_FOR_ZERO.ordinal()][Msg.CORRECT_PACKET_ZERO.ordinal()] = new ReceivePkt();
        transition[State.WAIT_FOR_ONE.ordinal()][Msg.CORRECT_PACKET_ONE.ordinal()] = new ReceivePkt();
        System.out.println("INFO FSM constructed, current state: " + currentState);
    }

    public void processMsg(Msg input) {
        System.out.println("INFO Received " + input + " in state " + currentState);
        Transition trans = transition[currentState.ordinal()][input.ordinal()];
        if (trans != null) {
            currentState = trans.execute(input);
        }
        System.out.println("INFO State: " + currentState);
    }


    abstract class Transition {
        abstract public State execute(Msg input);
    }

    class ReceivePkt extends Transition {
        @Override
        public State execute(Msg input) {
            System.out.println("Paket received");
            if (currentState == State.WAIT_FOR_ONE) {
                return State.WAIT_FOR_ZERO;
            } else {
                return State.WAIT_FOR_ONE;
            }
        }
    }


}
