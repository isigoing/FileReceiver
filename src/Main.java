public class Main {
    public static void main(String[] args) {
        new Thread(new FsmFileReceiver()).start();

//        String string = "abc.txt";
//        int index = string.indexOf(".");
//        String dataName = string.substring(0,index);
//        String dataEnding = string.substring(index,string.length());

    }
}
