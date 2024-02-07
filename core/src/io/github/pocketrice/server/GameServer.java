package io.github.pocketrice.server;

import com.esotericsoftware.kryonet.Server;
import io.github.pocketrice.client.GameClient;
import io.github.pocketrice.shared.Response;
import lombok.Getter;
import org.apache.commons.lang3.NotImplementedException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

@Getter
public abstract class GameServer {
    List<GameClient> clients;
    LinkedList<Object> outBuffer, inBuffer;
    Server kryoServer;
    Thread updateThread, kryoThread;
    String serverName;
    int tcpPort, tickRate, maxClients, maxOBufferSize, maxIBufferSize; // double-check if storing tcpPort is good practice
    

    public void setTcpPort(int port) throws IOException {
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

    public void ping() {
        throw new NotImplementedException();
    } // respond to client ping

    public abstract void start() throws IOException;
    public abstract void close();
    public abstract void connectClient(GameClient client);
    public abstract void disconnectClient(GameClient client);
    public abstract void sendPayload(String mid); // send out every-tick payload

    public abstract ServerPayload constructPayload(String mid);
    public abstract Response receivePayload(Object obj); // acknowledge received payload?? needed?

    @Override
    public String toString() {
        try {
            return this.getClass().getSimpleName() + "-" + InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
