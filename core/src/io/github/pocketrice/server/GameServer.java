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

    @Override
    public String toString() {
        try {
            return (!serverName.isEmpty()) ? serverName : this.getClass().getSimpleName() + "-" + InetAddress.getLocalHost() + ":" + tcpPort;
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
