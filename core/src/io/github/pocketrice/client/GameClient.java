package io.github.pocketrice.client;

import com.esotericsoftware.kryonet.Client;
import io.github.pocketrice.shared.Response;
import lombok.Getter;
import org.apache.commons.lang3.NotImplementedException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.LinkedList;

import static io.github.pocketrice.shared.AnsiCode.*;

@Getter
public abstract class GameClient {
    public static final long PING_INTERVAL = 2000;

    LinkedList<Object> outBuffer, inBuffer;
    Client kryoClient;
    Thread kryoThread, updateThread;
    InetSocketAddress serverAddress;
    Instant pingTime;
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

    public void log(String msg) {
        System.out.println("[GC] " + msg);
    }

    public void logErr(String msg) {
        System.out.println(ANSI_RED + "<!> [GC] " + msg + ANSI_RESET);
    }

    public void logWarn(String msg) {
        System.out.println(ANSI_YELLOW + "<?> [GC] " + msg + ANSI_RESET);
    }

    public void logInfo(String msg) {
        System.out.println(ANSI_BLUE + "<-> [GC] " + msg + ANSI_RESET);
    }

    public void logCon(String msg) {
        System.out.println(ANSI_PURPLE + "<x> [GC] " + msg + ANSI_RESET);
    }

    @Override
    public String toString() {
        try {
            return (!clientName.isEmpty()) ? clientName :  "client-" + InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
