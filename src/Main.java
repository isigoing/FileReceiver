public class Main {
    public static void main(String[] args) {
        FsmFileReceiver fsm = new FsmFileReceiver();
        fsm.processMsg(FsmFileReceiver.Msg.CORRECT_PACKET_ZERO);
        fsm.processMsg(FsmFileReceiver.Msg.CORRECT_PACKET_ONE);
        fsm.processMsg(FsmFileReceiver.Msg.CORRECT_PACKET_ZERO);

    }
}
