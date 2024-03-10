package io.github.pocketrice.client;

import com.esotericsoftware.kryonet.Client;
import io.github.pocketrice.shared.Orientation;
import io.github.pocketrice.shared.Response;
import lombok.Getter;
import org.apache.commons.lang3.NotImplementedException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;

import static io.github.pocketrice.shared.AnsiCode.*;

@Getter
public abstract class GameClient {
    public static final long PING_INTERVAL = 2000;

    LinkedList<Object> outBuffer, inBuffer;
    Client kryoClient;
    Thread kryoThread, updateThread;
    InetSocketAddress serverAddress;
    Instant pingTime, startTime;
    Player self;

    String clientName, serverName;
    int tcpPort, clientRate, maxOBufferSize, maxIBufferSize; // double-check if storing tcpPort  is good practice
    double serverTps;
    long ping;

    public boolean cleanBuffers() {
        int totalCleared = 0;

        while (inBuffer.size() > maxIBufferSize) {
            inBuffer.removeLast();
            totalCleared++;
        }

        while (outBuffer.size() > maxOBufferSize) {
            outBuffer.removeLast();
            totalCleared++;
        }

        return (totalCleared >= 0);
    }

    public boolean cleanBuffers(int amt) {
        int totalCleared = 0;

        for (int i = 0; !inBuffer.isEmpty() && i < amt; i++) {
            inBuffer.removeLast();
            totalCleared++;
        }

        for (int i = 0; !outBuffer.isEmpty() && i < amt; i++) {
            outBuffer.removeLast();
            totalCleared++;
        }

        return (totalCleared >= 0);
    }

    public void ping() {
        throw new NotImplementedException();
    }

    abstract void connect(int timeout, String ipv4, int[] ports) throws IOException;
    abstract void disconnect();
    abstract void reconnect() throws IOException;
    abstract void start(); // Not network related. This is instead for the GAME ITSELF!
    abstract void close();
    abstract void sendPayload(); // send out every-tick payload
    abstract PlayerPayload constructPayload();
    abstract PlayerTurnPayload constructTurnPayload();
    abstract Response receivePayload(Object obj); // acknowledge received payload?? needed?

    public void log(Object msg) {
        System.out.println(getLogTime() + " [GC] " + msg);
    }

    public void logErr(Object msg) {
        System.out.println(ANSI_RED + getLogTime() + " <!> [GC] " + msg + ANSI_RESET);
    }

    public void logWarn(Object msg) {
        System.out.println(ANSI_YELLOW + getLogTime() + " <?> [GC] " + msg + ANSI_RESET);
    }

    public void logInfo(Object msg) {
        System.out.println(ANSI_BLUE + getLogTime() + " <-> [GC] " + msg + ANSI_RESET);
    }

    public void logCon(Object msg) {
        System.out.println(ANSI_PURPLE + getLogTime() + " <x> [GC] " + msg + ANSI_RESET);
    }

    public void logValid(Object msg) {
        System.out.println(ANSI_GREEN + getLogTime() + " <✔> [GC] " + msg + ANSI_RESET);
    }

    public String getLogTime() {
        long hourSince = ChronoUnit.HOURS.between(startTime, Instant.now());
        long minSince = ChronoUnit.MINUTES.between(startTime, Instant.now()) - hourSince * 60;
        long secSince = ChronoUnit.SECONDS.between(startTime, Instant.now()) - minSince * 60;
        return (fillStr(String.valueOf(minSince), '0', 2) + ":" + fillStr(String.valueOf(secSince), '0', 2));
    }

    @Override
    public String toString() {
        try {
            return (!clientName.isEmpty()) ? clientName :  "client-" + InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static String fillStr(String str, char c, int len) {
        return fillStr(Orientation.LEFT, str, c, len);
    }

    public static String fillStr(Orientation or, String str, char c, int len) {
        StringBuilder sb = new StringBuilder(str);
        String fill = Character.toString(c).repeat(len - str.length());
        switch (or) {
            case LEFT -> sb.insert(0, fill);
            case RIGHT -> sb.append(fill);
            case CENTER -> {
                sb.insert(0, fill.substring(0, fill.length() / 2));
                sb.append(fill.substring(fill.length() / 2));
            }
        }

        return sb.toString();
    }
}
