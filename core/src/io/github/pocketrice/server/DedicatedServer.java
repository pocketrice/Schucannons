package io.github.pocketrice.server;

import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import io.github.pocketrice.client.GameClient;
import io.github.pocketrice.client.Match;
import io.github.pocketrice.client.PlayerPayload;
import io.github.pocketrice.server.Prysm.ForceConstant;
import io.github.pocketrice.shared.Request;
import io.github.pocketrice.shared.Response;
import io.github.pocketrice.shared.ResponseStatus;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.*;

public class DedicatedServer extends GameServer {
    Matchmaker mm;
    GameSimulator gsim;
    Map<String, Set<Connection>> clientMap; // todo: move to Matchmaker??

    public DedicatedServer() throws UnknownHostException {
        this(3074);
    }

    public DedicatedServer(int port) throws UnknownHostException {
        this("server-" + InetAddress.getLocalHost(), port);
    }

    public DedicatedServer(String name, int port) {
        serverName = name;
        tcpPort = port;
        kryoServer = new Server();
        gsim = new GameSimulator();
        clients = new ArrayList<>();
        clientMap = new TreeMap<>();
        inBuffer = new LinkedList<>();
        outBuffer = new LinkedList<>();

        tickRate = 66;
        maxClients = 20;
        maxIBufferSize = maxOBufferSize = 10;
    }

    // This should be done (opt'l) prior to closing a server.
    public void transfer(GameServer server) {
        System.out.println("Clients moved from " + this.serverName + " to " + server.serverName);
        for (GameClient cl : clients) {
            server.connectClient(cl);
        }
    }

    public PlayerPayload findMostRecentPayload(String pid) {
        PlayerPayload pp = null;
        for (int i = 0; pp == null && i < inBuffer.size(); i++) {
            if (((PlayerPayload) inBuffer.get(i)).getPlayerId().matches(pid)) {
                pp = (PlayerPayload) inBuffer.get(i);
            }
        }

        return pp;
    }

    @Override
    public void start() throws IOException {
        kryoServer.bind(tcpPort);
        kryoServer.start();
        kryoServer.addListener(new Listener() {
            public void received (Connection con, Object obj) {
                if (obj instanceof Request rq) {
                    switch (rq.getMsg()) {
                        case "GC_matches" -> {
                            kryoServer.sendToTCP(con.getID(), new Response("GS_matches", mm.matches));
                        }

                        case "GC_selMatch" -> {
                            String mid = (String) rq.getPayload();
                            clientMap.putIfAbsent(mid, new HashSet<>());
                            clientMap.get(mid).add(con);
                            kryoServer.sendToTCP(con.getID(), new Response("GS_status", ResponseStatus.OK));
                        }
                    }
                }
            }
        });

        Thread updateThread = new Thread(() -> { // Updates every tick (66 Hz).
            while (true) {
                try {
                    wait(tickRate);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                for (Match m : mm.matches) {
                    sendPayload(m.getIdentifier());
                }
            }
        });

        updateThread.start();

    }

    @Override
    public void close() {
        kryoServer.close();
    }

    @Override
    void setTcpPort(int port) throws IOException {
        tcpPort = port;
        kryoServer.close();
        kryoServer.bind(tcpPort);
        kryoServer.start();
    }

    @Override
    Response ping() {
        return new Response(ResponseStatus.OK.name(), null);
    }

    @Override
    public void connectClient(GameClient client) {
        ResponseStatus status;
        if (clients.size() < maxClients) {
            clients.add(client);
            status = ResponseStatus.OK;
        } else {
            status = ResponseStatus.FAIL;
        }
        kryoServer.sendToTCP(client.getKryoClient().getID(), new Response(status.name(), null));

        // TODO: connect client to krs :(
    }

    @Override
    public void disconnectClient(GameClient client) {
        ResponseStatus status = (clients.remove(client)) ? ResponseStatus.OK : ResponseStatus.FAIL;
        kryoServer.sendToTCP(client.getKryoClient().getID(), new Response(status.name(), null)); // still need response?
        client.getKryoClient().stop();


        // TODO: disconnect client from krs :(
    }

    @Override
    public void sendPayload(String mid) {
        ServerPayload sp = constructPayload(mid);
        outBuffer.addFirst(sp);
        cleanBuffers();
        sendToMatch(mid, new Response("GS_pl", sp));
    }

    public void sendToMatch(String mid, Response resp) {
        for (Connection con : clientMap.get(mid)) {
            kryoServer.sendToTCP(con.getID(), resp);
        }
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
    public ServerPayload constructPayload(String mid) {
        Match m = mm.findMatch(mid);
        String a_id = String.valueOf(m.getCurrentPlayer().getPlayerId());
        String b_id = String.valueOf(m.getOppoPlayer().getPlayerId());

        PlayerPayload a_pp = findMostRecentPayload(a_id);
        PlayerPayload b_pp = findMostRecentPayload(b_id);

        Vector3 a_cpos = a_pp.getCannonPos(); // todo: interp?
        Vector3 b_cpos = b_pp.getCannonPos();

        Vector3 a_pmv = a_pp.getProjMotVec();
        Vector3 b_pmv = b_pp.getProjMotVec();

        // ∆x = v0x*t
        // ∆y = v0y*t - 0.5gt^2
        // ∆z = v0z*t


        float t = Instant.from(a_pp.getTimestamp()).toEpochMilli();
        float x = a_pmv.x * t;
        float y = (float) (a_pmv.y * t - 0.5 * ForceConstant.EARTH_G.val() * t * t);
        float z = a_pmv.z * t;

        Vector3 cballpos = a_pp.getCannonPos().add(x,y,z); // todo: validate if this is right.

        return new ServerPayload(mid, a_id, b_id, a_cpos, b_cpos, a_pmv, b_pmv, cballpos);
    }


    @Override
    public Response receivePayload(Object obj) {
        inBuffer.addFirst(obj); // Most recent = first
        cleanBuffers();

        return new Response(ResponseStatus.OK.name(), null);
    }
}
