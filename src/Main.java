
public class Main {
    public static void main(String[] args) {
        new Thread(new FsmFileReceiver()).start();
        //new Thread(new TestSender()).start();
    }
}
