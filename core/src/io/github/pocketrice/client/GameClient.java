package io.github.pocketrice.client;

import com.esotericsoftware.kryonet.Client;
import io.github.pocketrice.server.GameServer;
import io.github.pocketrice.shared.Request;
import io.github.pocketrice.shared.Response;
import lombok.Getter;

import java.io.IOException;
import java.util.LinkedList;

@Getter
public abstract class GameClient {
    GameServer server;
    LinkedList<Object> outBuffer, inBuffer;
    Client kryoClient;
    String clientName;
    int tcpPort, clientRate, maxOBufferSize, maxIBufferSize; // double-check if storing tcpPort  is good practice

    abstract void connect(int timeout, String ipv4, int[] ports) throws IOException;
    abstract void disconnect();
    abstract void reconnect() throws IOException;
    abstract Request ping(); // send a ping
    abstract void start(); // Not network related. This is instead for the GAME ITSELF!
    abstract void close();
    abstract void sendPayload(); // send out every-tick payload
    abstract boolean cleanBuffers();
    abstract boolean cleanBuffers(int amt);
    abstract PlayerPayload constructPayload();
    abstract PlayerTurnPayload constructTurnPayload();
    abstract Response receivePayload(Object obj); // acknowledge received payload?? needed?
}
