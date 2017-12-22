import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class FsmFileReceiver implements Runnable {
    private int seq;
    private long checksum;
    private int contentLength;
    private File file;
    private boolean fileExists = false;
    private InetAddress returnAdress;
    private int returnPort;


    @Override
    public void run() {


        int port = 10_000;
        byte[] pkt = new byte[1213];
        byte[] data = new byte[1200];

        Checksum checker = new CRC32();


        try {
            DatagramSocket receiverSocket = new DatagramSocket(port);
            receiverSocket.setSoTimeout(10_000);
            DatagramPacket packet = new DatagramPacket(pkt, pkt.length);
//            FileOutputStream fop = new FileOutputStream(file);
            System.out.println("Waiting for packets...");
            try {
                while (true) {
                    receiverSocket.receive(packet);
                    extractPkt(data, packet);
                    checker.update(data, 0, data.length);


                    //toDo aufpassen dass nur eins ausgeführt wird !!!
                    if (currentState == State.WAIT_FOR_ZERO &&
                            checksum == checker.getValue() &&
                            seq == 0) {
                        //toDo deliver data, send ack, changes State
                        processMsg(Msg.CORRECT_PACKET_ZERO);
                    } else if (currentState == State.WAIT_FOR_ZERO &&
                            (checksum != checker.getValue() ||
                                    seq == 1)) {
                        //toDo send ack again
                        processMsg(Msg.CORRUPT_PACKET);
                    } else if (currentState == State.WAIT_FOR_ONE &&
                            checksum == checker.getValue() &&
                            seq == 1) {
                        //toDo deliver data, send ack, changes State
                        processMsg(Msg.CORRECT_PACKET_ONE);
                    } else if (currentState == State.WAIT_FOR_ONE &&
                            (checksum != checker.getValue() ||
                                    seq == 0)) {
                        //toDo send ack again
                        processMsg(Msg.CORRUPT_PACKET);
                    }


                }
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout Exception");
            } catch (Exception e) {
                System.out.println("Good Morning Exception");
                e.printStackTrace();
            }

        } catch (SocketException  e) {
            e.printStackTrace();
        }
    }

    private void extractPkt(byte[] data, DatagramPacket packet) throws IOException {
        System.out.println("Start");

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(packet.getData()));
        returnAdress = packet.getAddress();
        returnPort = packet.getPort();

        // Read SEQ 0/1
        seq = in.read();
        System.out.println("seq " + seq);

        // Combine Content Length Bytes
        byte[] check = new byte[8];
        for (int i = 0; i < check.length; i++) {
            check[i] = in.readByte();
            System.out.println("checksum (for loop) " + check[i]);
        }
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(check);
        buffer.flip();
        checksum = buffer.getLong();
        System.out.println("checksum " + checksum);

        // Combine Content Length Bytes
        byte[] contentL = new byte[4];
        for (int i = 0; i < contentL.length; i++) {
            contentL[i] = in.readByte();
            System.out.println("content Length (for loop) " + contentL[i]);
        }
        ByteBuffer buffer2 = ByteBuffer.allocate(Long.BYTES);
        buffer2.put(contentL);
        buffer2.flip();
        contentLength = buffer2.getInt();
        System.out.println("content Length " + contentLength);
        System.out.println("data length " + data.length);

        // Control Output for data
        for (int i = 0; i < data.length; i++) {
            data[i] = in.readByte();
            System.out.println("data " + data[i]);
        }




        // Save data to File or create a new File
        if (!fileExists) {

            char[] charBuffer = new char[contentLength];
            for (int i = 0; i < contentLength; i++) {
                charBuffer[i] = (char) data[i];
                System.out.println(charBuffer[i]);
            }
            String string = new String(charBuffer);
            System.out.println(String.valueOf(string));
            file = new File(string);

            FileOutputStream fop = new FileOutputStream(file);
            fop.write(data);
            fop.flush();
            fop.close();

            fileExists = true;

        } else {

            if(contentLength == 1200){
                FileOutputStream fop = new FileOutputStream(file,true);
                fop.write(data);
                fop.flush();
                fop.close();
            } else {

                byte[] newData = new byte[contentLength];
                for (int i = 0; i < newData.length; i++) {
                    newData[i] = data[i];
                    System.out.println(newData[i]);
                }
                FileOutputStream fop = new FileOutputStream(file,true);
                fop.write(newData);
                fop.flush();
                fop.close();


            }
        }



        System.out.println("End");
    }


    // all states for this FSM
    enum State {
        WAIT_FOR_ZERO, WAIT_FOR_ONE
    }

    // all messages/conditions which can occur
    enum Msg {
        CORRECT_PACKET_ONE, CORRECT_PACKET_ZERO, CORRUPT_PACKET
    }

    private State currentState;
    private Transition[][] transition;

    public FsmFileReceiver() {
        currentState = State.WAIT_FOR_ZERO;
        transition = new Transition[State.values().length][Msg.values().length];
        transition[State.WAIT_FOR_ZERO.ordinal()][Msg.CORRECT_PACKET_ZERO.ordinal()] = new ReceivePkt();
        transition[State.WAIT_FOR_ZERO.ordinal()][Msg.CORRUPT_PACKET.ordinal()] = new ResendAck();
        transition[State.WAIT_FOR_ONE.ordinal()][Msg.CORRECT_PACKET_ONE.ordinal()] = new ReceivePkt();
        transition[State.WAIT_FOR_ONE.ordinal()][Msg.CORRUPT_PACKET.ordinal()] = new ResendAck();
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
                sendAck(1);
                return State.WAIT_FOR_ZERO;
            } else {
                sendAck(0);
                return State.WAIT_FOR_ONE;
            }
        }
    }

    class ResendAck extends Transition {
        @Override
        public State execute(Msg input) {
            System.out.println("Resend Ack");
            if (currentState == State.WAIT_FOR_ONE) {
                sendAck(0);
                return currentState;
            } else {
                sendAck(1);
                return currentState;
            }

        }
    }

    private void sendAck(int number) {
        byte[] data = new byte[0];
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);
        try {
            out.write(number);
            Checksum checksum = new CRC32();
            checksum.update(data, 0, data.length);
            out.writeLong(checksum.getValue());
            out.write(data);
            data = byteOut.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            DatagramSocket socket = new DatagramSocket();
//          InetAddress ia = InetAddress.getLocalHost();
            DatagramPacket packet = new DatagramPacket(data, data.length, returnAdress, 9000);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
