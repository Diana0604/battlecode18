import bc.*;
import java.util.ArrayList;

public class Communication {

    public static final int MAX_MESSAGES = 99;
    public static final int ROCKET_LOC = 0;
    public static final int subjectBits = 3;

    private static Communication instance;
    private ArrayList<Integer> messagesToSend;
    private ArrayList<Message> messagesToRead;

    public static Communication getInstance() {
        if (instance == null) instance = new Communication();
        return instance;
    }

    public static void initTurn() {
        getInstance().initInstanceTurn();
    }

    public void initInstanceTurn() {
        messagesToSend = new ArrayList<>();
        messagesToRead = new ArrayList<>();

        Planet otherPlanet = Planet.Mars;
        if (Mapa.onMars()) otherPlanet = Planet.Earth;
        Veci32 otherPlanetArray = GC.gc.getTeamArray(otherPlanet);
        int M = Math.min(MAX_MESSAGES, otherPlanetArray.get(0));
        for (int i = 0; i < M; ++i) {
            messagesToRead.add(decode(otherPlanetArray.get(i+1)));
        }
        otherPlanetArray.delete();
    }

    public static void endTurn() {
        getInstance().doSendMessages();
    }

    public void sendRocketLocation(AuxMapLocation loc) {
        sendLocation(ROCKET_LOC, loc);
    }

    private void sendLocation(int subject, AuxMapLocation loc) {
        int body = loc.x << 6 | loc.y;
        sendMessage(subject, body);
    }

    private void sendMessage(int subject, int body) {
        if (subject >= 1<<subjectBits) System.out.println("El subject es passa de bits");
        int msg = (subject << (32-subjectBits)) | body;
        messagesToSend.add(msg);
    }

    public boolean canSendMessage() {
        return messagesToSend.size() < MAX_MESSAGES;
    }

    public void doSendMessages() {
        int N = messagesToSend.size();
        GC.gc.writeTeamArray(0, N);
        for (int i = 0; i < N; ++i) {
            GC.gc.writeTeamArray(i+1, messagesToSend.get(i));
        }
    }

    public ArrayList<Message> getMessagesToRead() {
        return messagesToRead;
    }

    static Message decode(int msg_i) {
        Message msg = new Message();
        msg.subject = (msg_i >> (32-subjectBits)) & ((1<<(subjectBits))-1);
        msg.body = msg_i & ((1<<(32-subjectBits))-1);
        if (msg.subject == ROCKET_LOC) {
            msg.mapLoc = new AuxMapLocation((msg.body >> 6) & 0x3F, msg.body&0x3F);
        }
        return msg;
    }


}