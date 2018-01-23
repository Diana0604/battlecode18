import bc.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class Communication {

    public static final int MAX_MESSAGES = 99;
    public static final int ROCKET_LOC = 0;
    public static final int subjectBits = 3;

    private static Communication instance;
    private ArrayList<Integer> messagesToSend;
    private ArrayList<Integer> messagesToRead;

    public Communication getInstance() {
        if (instance == null) instance = new Communication();
        return instance;
    }

    public void initTurn() {
        messagesToSend = new ArrayList<>();
        messagesToRead = new ArrayList<>();

        Planet otherPlanet = Planet.Mars;
        if (Data.planet == Planet.Mars) otherPlanet = Planet.Earth;
        Veci32 otherPlanetArray = Data.gc.getTeamArray(otherPlanet);
        int M = Math.min(MAX_MESSAGES, otherPlanetArray.get(0));
        for (int i = 0; i < M; ++i) {
            messagesToRead.add(otherPlanetArray.get(i+1));
        }
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
        Data.gc.writeTeamArray(0, N);
        for (int i = 0; i < N; ++i) {
            Data.gc.writeTeamArray(i+1, messagesToSend.get(i));
        }
    }

    public ArrayList<Integer> getMessagesToRead() {
        return messagesToRead;
    }

}