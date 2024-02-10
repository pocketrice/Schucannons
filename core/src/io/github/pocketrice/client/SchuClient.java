package io.github.pocketrice.client;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import io.github.pocketrice.shared.KryoInitialiser;
import io.github.pocketrice.shared.Request;
import io.github.pocketrice.shared.Response;
import io.github.pocketrice.shared.ResponseStatus;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.LinkedList;
import java.util.UUID;


public class SchuClient extends GameClient {
    String matchId;
    GameManager gmgr;


    public SchuClient(GameManager gm) throws UnknownHostException {
        this(gm, 3074);
    }

    public SchuClient(GameManager gm, int port) throws UnknownHostException {
        this(gm, "client-" + InetAddress.getLocalHost(), port);
    }

    public SchuClient(GameManager gm, String name, int port) {
        clientName = name;
        tcpPort = port;
        kryoClient = new Client();
        gmgr = gm;
        self = new HumanPlayer(UUID.randomUUID(), "NOTBOT");

        inBuffer = new LinkedList<>();
        outBuffer = new LinkedList<>();

        clientRate = calcClientRate();
        maxIBufferSize = maxOBufferSize = 10;
    }

    public int calcClientRate() {
        //ping();
        return 20; // todo: get ping packet and do stuffs
    }

    public void start() {
        KryoInitialiser.registerClasses(kryoClient.getKryo());

        kryoThread = new Thread(() -> {
            kryoClient.start();
            kryoClient.addListener(new Listener() {
                public void received(Connection con, Object obj) {
                    if (obj instanceof Response rp) {
                        switch (rp.getMsg()) {
                            case "GS_matches" -> {
                                gmgr.receiveMatchList(rp.getPayload());
                            }

                            case "GS_selMatch" -> {
                                gmgr.receiveMatch(rp.getPayload());
                                gmgr.setClientConnected(true);
                            }

                            case "GS_mid" -> {
                                gmgr.receiveMatchId(rp.getPayload());
                            }

                            case "GS_plList" -> {
                                gmgr.receivePlayerList(rp.getPayload());
                            }
                            case "GS_pl" -> {
                                System.out.println(rp.getPayload());
                                gmgr.receiveServerUpdate(rp.getPayload());
                            }

                            case "GS_ping" -> {
                                kryoClient.sendTCP(new Response("GC_ping", Instant.now()));
                            }
                        }
                    }
                }
            });
        });

        kryoThread.setName(this + "-KryoThread");
        kryoThread.start();
    }

    @Override
    public void connect(int timeout, String ipv4, int[] ports) throws IOException {
        kryoClient.connect(timeout, ipv4, ports[0]);
        kryoClient.sendTCP(new Request("GC_connect", null)); // needed?
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
    public void close() {

    }

    @Override
    public void sendPayload() {
        PlayerPayload pp = constructPayload();
        outBuffer.addFirst(pp);
        cleanBuffers();
        kryoClient.sendTCP(new Request("GC_pp", pp));
    }


    @Override
    public PlayerPayload constructPayload() {
        return new PlayerPayload(self.getIdentifier(), matchId, self.getPos(), self.projVector);
    }

    @Override
    public PlayerTurnPayload constructTurnPayload() {
        return new PlayerTurnPayload(self.projVector);
    }

    @Override
    public Response receivePayload(Object obj) {
        inBuffer.addFirst(obj); // Most recent = first
        cleanBuffers();

        return new Response(ResponseStatus.OK.name(), null);
    }
}
