import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.zip.CRC32;

public class FsmFileReceiver implements Runnable {
    private int seq;
    private long checksum;
    private int contentLength;
    private File file;
    private boolean fileExists = false;
    private InetAddress returnAddress;
    private int counter = 0;
    private double random;
    private double duplicateChance = 0.05;
    private double bitErrorChance = 0.05;
    private double loseChance = 0.1;
    private int duplicatePacket;
    private int bitErrorPacket;
    private int lostPacket;
    private boolean duplicate = false;
    private boolean bitError = false;
    private boolean loss = false;
    private long startTime;
    private long endTime;


    @Override
    public void run() {

        int port = 10_000;
        byte[] pkt = new byte[1213];
        byte[] data = new byte[1200];

        try {
            DatagramSocket receiverSocket = new DatagramSocket(port);
            DatagramPacket packet = new DatagramPacket(pkt, pkt.length);
            System.out.println("Server started: Waiting for packets...");
            try {
                while (true) {

                    CRC32 checker = new CRC32();
                    random = Math.random();
                    if (random < loseChance) {
                        loss = true;
                    } else if (random < loseChance + duplicateChance) {
                        duplicate = true;
                    } else if (random < loseChance + duplicateChance + bitErrorChance) {
                        bitError = true;
                    }

                    receiverSocket.receive(packet);
                    if(!fileExists){
                        startTime = System.currentTimeMillis();
                    }

                    if (duplicate) {
                        duplicatePacket++;
                    } else {
                        extractPkt(data, packet);
                    }
                    checker.reset();
                    checker.update(data, 0, contentLength);

                    if (currentState == State.WAIT_FOR_ZERO && checksum == checker.getValue() && seq == 0) {

                        extractData(data);
                        processMsg(Msg.OK_PACKET_ZERO);

                    } else if (currentState == State.WAIT_FOR_ZERO && (checksum != checker.getValue() || seq == 1)) {

                        processMsg(Msg.CORRUPT_PACKET);

                    } else if (currentState == State.WAIT_FOR_ONE && checksum == checker.getValue() && seq == 1) {

                        extractData(data);
                        processMsg(Msg.OK_PACKET_ONE);

                    } else if (currentState == State.WAIT_FOR_ONE && (checksum != checker.getValue() || seq == 0)) {

                        processMsg(Msg.CORRUPT_PACKET);
                    }


                    if(currentState == State.WAIT_FOR_ONE && !fileExists){
                        processMsg(Msg.RESET);
                    }


                    duplicate = false;
                    bitError = false;
                    loss = false;
                }


            } catch (SocketTimeoutException e) {
                System.out.println("    Timeout Exception");
            } catch (Exception e) {
                System.out.println("    Good Morning Exception");
                e.printStackTrace();
            }

        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void extractPkt(byte[] data, DatagramPacket packet) throws IOException {
        System.out.println("    Beginning of extractPkt");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(packet.getData()));
        returnAddress = packet.getAddress();
        if (loss) {

            int seqLoss = in.read();
            byte[] checkLoss = new byte[8];
            for (int i = 0; i < checkLoss.length; i++) {
                checkLoss[i] = in.readByte();
            }
            byte[] contentLengthLoss = new byte[4];
            for (int i = 0; i < contentLengthLoss.length; i++) {
                contentLengthLoss[i] = in.readByte();
            }
            byte[] dataLoss = new byte[1200];
            for (int i = 0; i < dataLoss.length; i++) {
                dataLoss[i] = in.readByte();
            }
            lostPacket++;

        } else {

            counter++;


            // Read SEQ 0/1
            seq = in.read();

            // Combine Content Length Bytes
            byte[] check = new byte[8];
            for (int i = 0; i < check.length; i++) {
                check[i] = in.readByte();
            }
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.clear();
            buffer.put(check);
            buffer.flip();
            checksum = buffer.getLong();

            // Combine Content Length Bytes
            byte[] contentL = new byte[4];
            for (int i = 0; i < contentL.length; i++) {
                contentL[i] = in.readByte();
            }
            ByteBuffer buffer2 = ByteBuffer.allocate(Long.BYTES);
            buffer2.put(contentL);
            buffer2.flip();
            contentLength = buffer2.getInt();


            // Fill Data with Bytes
            for (int i = 0; i < data.length; i++) {
                data[i] = in.readByte();
            }

            if (bitError) {
                Random rand = new Random();
                int randomNum = rand.nextInt((data.length) + 1);
                data[randomNum] = (byte) ~data[randomNum];
                bitErrorPacket++;
            }


            System.out.println("    Number of received packets: " + counter);
            System.out.println("    End of extractPkt");
        }
    }

    private void extractData(byte[] data) throws IOException {
        if (!fileExists) {
            counter = 1;

            char[] charBuffer = new char[contentLength];
            for (int i = 0; i < contentLength; i++) {
                charBuffer[i] = (char) data[i];
            }
            String string = new String(charBuffer);
            file = new File(string);

            fileExists = true;

        } else {

            if (contentLength == 1200) {
                FileOutputStream fop = new FileOutputStream(file, true);
                fop.write(data);
                fop.flush();
                fop.close();
            } else {

                byte[] newData = new byte[contentLength];
                for (int i = 0; i < newData.length; i++) {
                    newData[i] = data[i];
                }
                FileOutputStream fop = new FileOutputStream(file, true);
                fop.write(newData);
                fop.flush();
                fop.close();

                fileExists = false;
                printOccuredErrors();
                printThroughPut();

            }
        }
    }

    private void printThroughPut() {
        double time;
        endTime = System.currentTimeMillis();
        time = (endTime-startTime)/1000;
        double sizeMbit = (file.length()* 8) /1_000_000;
        System.out.println("        ##################################");
        System.out.println("        ##  ThroughPut:       " + sizeMbit/time +" Mbits/s");
        System.out.println("        ##################################");
    }

    private void printOccuredErrors() {
        System.out.println("        ##################################");
        System.out.println("        ##  lost Packets:       " + lostPacket);
        System.out.println("        ##  duplicate Packets:  " + duplicatePacket);
        System.out.println("        ##  bit Error Packets:  " + bitErrorPacket);
        System.out.println("        ##################################");
    }


    // all states for this FSM
    enum State {
        WAIT_FOR_ZERO, WAIT_FOR_ONE
    }

    // all messages/conditions which can occur
    enum Msg {
        OK_PACKET_ONE, OK_PACKET_ZERO, CORRUPT_PACKET, RESET
    }

    private State currentState;
    private Transition[][] transition;

    public FsmFileReceiver() {
        currentState = State.WAIT_FOR_ZERO;
        transition = new Transition[State.values().length][Msg.values().length];
        transition[State.WAIT_FOR_ZERO.ordinal()][Msg.OK_PACKET_ZERO.ordinal()] = new ReceivePkt();
        transition[State.WAIT_FOR_ZERO.ordinal()][Msg.CORRUPT_PACKET.ordinal()] = new ResendAck();
        transition[State.WAIT_FOR_ONE.ordinal()][Msg.OK_PACKET_ONE.ordinal()] = new ReceivePkt();
        transition[State.WAIT_FOR_ONE.ordinal()][Msg.CORRUPT_PACKET.ordinal()] = new ResendAck();
        transition[State.WAIT_FOR_ONE.ordinal()][Msg.RESET.ordinal()] = new Reset();
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
            System.out.println("    Paket received");
            if (currentState == State.WAIT_FOR_ONE) {
                sendAck(1);
                System.out.println("    Send Ack 1");
                return State.WAIT_FOR_ZERO;
            } else {
                sendAck(0);
                System.out.println("    Send Ack 0");
                return State.WAIT_FOR_ONE;
            }
        }
    }

    class ResendAck extends Transition {
        @Override
        public State execute(Msg input) {
            if (currentState == State.WAIT_FOR_ONE) {
                sendAck(0);
                System.out.println("    Resend Ack 0");
                return currentState;
            } else {
                sendAck(1);
                System.out.println("    Resend Ack 1");
                return currentState;
            }

        }
    }

    class Reset extends Transition {
        @Override
        public State execute(Msg input) {
            return State.WAIT_FOR_ZERO;
        }
    }

    private void sendAck(int number) {
        byte[] data = new byte[0];
        byte[] numberByte = new byte[1];
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteOut);
        numberByte[0] = (byte) number;
        try {
            out.write(number);
            CRC32 ackChecksum = new CRC32();
            ackChecksum.reset();
            ackChecksum.update(numberByte, 0, 1);
            out.writeLong(ackChecksum.getValue());
            out.write(data);
            data = byteOut.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            DatagramSocket socket = new DatagramSocket();
//            InetAddress ia = InetAddress.getLocalHost();
            DatagramPacket packet = new DatagramPacket(data, data.length, returnAddress, 9000);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
