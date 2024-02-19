package io.github.pocketrice.server;

import com.esotericsoftware.kryonet.Server;
import io.github.pocketrice.client.GameClient;
import io.github.pocketrice.shared.Response;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static io.github.pocketrice.shared.AnsiCode.*;
import static io.github.pocketrice.shared.AnsiCode.ANSI_RESET;

@Getter
public abstract class GameServer {
    List<GameClient> clients;
    LinkedList<Object> outBuffer, inBuffer;
    Server kryoServer;
    Thread updateThread, kryoThread;
    String serverName;
    int tcpPort, tickRate, tickCounter, maxClients, maxOBufferSize, maxIBufferSize; // double-check if storing tcpPort is good practice
    @Setter
    double tps;

    public void changePort(int port) throws IOException {
        tcpPort = port;
        kryoServer.close();
        kryoServer.bind(tcpPort);
        kryoServer.start();
    }

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

    public abstract void start() throws IOException;
    public abstract void close();
    public abstract void connectClient(GameClient client);
    public abstract void disconnectClient(GameClient client);
    public abstract void sendPayload(UUID mid); // send out every-tick payload

    public abstract ServerPayload constructPayload(UUID mid);
    public abstract Response receivePayload(Object obj); // acknowledge received payload?? needed?

    public void log(String msg) {
        System.out.println("[GS] " + msg);
    }

    public void logErr(String msg) {
        System.out.println(ANSI_RED + "<!> [GS] " + msg + ANSI_RESET);
    }

    public void logWarn(String msg) {
        System.out.println(ANSI_YELLOW + "<?> [GS] " + msg + ANSI_RESET);
    }

    public void logInfo(String msg) {
        System.out.println(ANSI_BLUE + "<-> [GS] " + msg + ANSI_RESET);
    }

    public void logCon(String msg) {
        System.out.println(ANSI_PURPLE + "<x> [GS] " + msg + ANSI_RESET);
    }

    @Override
    public String toString() {
        try {
            return (!serverName.isEmpty()) ? serverName : this.getClass().getSimpleName() + "-" + InetAddress.getLocalHost() + ":" + tcpPort;
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
