package io.github.pocketrice.client;

import com.esotericsoftware.kryonet.Client;
import io.github.pocketrice.shared.Request;
import io.github.pocketrice.shared.Response;
import io.github.pocketrice.shared.ResponseStatus;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;

public class SchuClient extends GameClient {
    String matchId;


    public SchuClient() throws UnknownHostException {
        this(3074);
    }

    public SchuClient(int port) throws UnknownHostException {
        this("client-" + InetAddress.getLocalHost(), port);
    }

    public SchuClient(String name, int port) {
        clientName = name;
        tcpPort = port;
        kryoClient = new Client();

        inBuffer = new LinkedList<>();
        outBuffer = new LinkedList<>();

        clientRate = calcClientRate();
        maxIBufferSize = maxOBufferSize = 10;
    }

    public int calcClientRate() {
        ping();
        return server.getTickRate(); // todo use ping crap to calc something
    }

    public void start() {

    }

    @Override
    public void connect(int timeout, String ipv4, int[] ports) throws IOException {
        kryoClient.connect(timeout, ipv4, ports[0]);
        kryoClient.sendTCP(new Request("pls thank u", null)); // needed?
    }

    @Override
    public void disconnect() {
        kryoClient.sendTCP(new Response(ResponseStatus.OK.name(), null)); // needed?
        kryoClient.stop();
    }

    @Override
    public void reconnect() throws IOException {
        kryoClient.reconnect();
    }

    @Override
    public Request ping() {

        return null;
    }


    @Override
    public void close() {

    }

    @Override
    public void sendPayload() {
        PlayerPayload pp = constructPayload();
        outBuffer.addFirst(pp);
        cleanBuffers();
        kryoClient.sendTCP(pp);
    }

    @Override
    public boolean cleanBuffers() {
        int totalCleared = 0;

        while (inBuffer.size() < maxIBufferSize) {
            inBuffer.removeLast();
            totalCleared++;
        }

        while (outBuffer.size() < maxOBufferSize) {
            outBuffer.removeLast();
            totalCleared++;
        }

        return (totalCleared >= 0);
    }

    @Override
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

    @Override
    public PlayerPayload constructPayload() {
        return null;
    }

    @Override
    public PlayerTurnPayload constructTurnPayload() {

    }

    @Override
    public Response receivePayload(Object obj) {
        return null;
    }
}
