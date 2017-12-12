import com.sun.org.apache.xpath.internal.SourceTree;

public class FsmFileReceiver {
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
