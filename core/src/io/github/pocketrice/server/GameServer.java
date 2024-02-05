package io.github.pocketrice.server;

import com.esotericsoftware.kryonet.Server;
import io.github.pocketrice.client.GameClient;
import io.github.pocketrice.shared.Response;
import lombok.Getter;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

@Getter
public abstract class GameServer {
    List<GameClient> clients;
    LinkedList<Object> outBuffer, inBuffer;
    Server kryoServer;
    String serverName;
    int tcpPort, tickRate, maxClients, maxOBufferSize, maxIBufferSize; // double-check if storing tcpPort is good practice
    

    abstract void start() throws IOException;
    abstract void close();
    abstract void setTcpPort(int port) throws IOException;
    abstract Response ping(); // respond to client ping
    abstract void connectClient(GameClient client);
    abstract void disconnectClient(GameClient client);
    abstract void sendPayload(String mid); // send out every-tick payload

    abstract boolean cleanBuffers();
    abstract boolean cleanBuffers(int amt);

    abstract ServerPayload constructPayload(String mid);
    abstract Response receivePayload(Object obj); // acknowledge received payload?? needed?
}
