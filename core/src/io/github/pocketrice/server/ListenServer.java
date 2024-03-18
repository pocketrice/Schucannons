package io.github.pocketrice.server;

import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import io.github.pocketrice.client.GameClient;
import io.github.pocketrice.client.Match;
import io.github.pocketrice.client.PlayerPayload;
import io.github.pocketrice.server.Prysm.ForceConstant;
import io.github.pocketrice.shared.KryoInitialiser;
import io.github.pocketrice.shared.Request;
import io.github.pocketrice.shared.Response;
import io.github.pocketrice.shared.ResponseStatus;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;

// A listenserver runs on the local machine; it should only run 1 MATCH. So, it can be slightly optimised.
public final class ListenServer extends GameServer {
    Match m;
    GameSimulator gsim;

    public ListenServer() throws UnknownHostException {
        this(3074);
    }

    public ListenServer(int port) throws UnknownHostException {
        this("", port);
    }

    public ListenServer(String name, int port) {
        serverName = (name.isBlank()) ? this.toString() : name;
        tcpPort = port;
        kryoServer = new Server();

        m = new Match();
        gsim = new GameSimulator();
        clients = new ArrayList<>();
        inBuffer = new LinkedList<>();
        outBuffer = new LinkedList<>();

        tickRate = 20;
        maxClients = 2 + Match.MAX_SPECTATORS;
        maxIBufferSize = maxOBufferSize = 10;
    }

    // This should be done (opt'l) prior to closing a server.
    public void transfer(GameServer server) {
        System.out.println("Clients moved from " + this.serverName + " to " + server.serverName);
        for (GameClient cl : clients) {
            server.connectClient(cl);
        }
    }

    public PlayerPayload findMostRecentPayload(UUID pid) {
        PlayerPayload pp = null;
        for (int i = 0; pp == null && i < inBuffer.size(); i++) {
            if (((PlayerPayload) inBuffer.get(i)).getPlayerId().equals(pid)) {
                pp = (PlayerPayload) inBuffer.get(i);
            }
        }

        return pp;
    }

    @Override
    public void start() throws IOException {
        KryoInitialiser.registerClasses(kryoServer.getKryo());

        kryoServer.bind(tcpPort);
        kryoThread = new Thread(() -> {
            kryoServer.start();
            kryoServer.addListener(new Listener() {
                public void received (Connection con, Object obj) {
                    if (obj instanceof Request rq) {
                        switch (rq.getMsg()) {
                            case "GC_matches" -> {
                                kryoServer.sendToTCP(con.getID(), new Response("GS_matches", m.getIdentifier()));
                            }

                            case "GC_selMatch" -> {
                                // Clients should not send a selMatch packet. This is more so for my testing, to avoid that needless overhead.
                                kryoServer.sendToTCP(con.getID(), new Response("GS_selMatch", constructPayload(m.getMatchId())));
                                kryoServer.sendToTCP(con.getID(), new Response("GS_mid", m.getMatchId() + "|" + m.getMatchName()));
                            }
                        }
                    }
                }
            });
        });

        kryoThread.start();

        updateThread = new Thread(() -> { // Updates every tick (66 Hz).
            while (true) {
                try {
                    wait(tickRate);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("updated");
                sendPayload(m.getMatchId());
            }
        });

        updateThread.start();

    }

    @Override
    public void close() {
        kryoServer.close();
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
    public void sendPayload(UUID mid) {
        ServerPayload sp = constructPayload(mid);
        outBuffer.addFirst(sp);
        cleanBuffers();
        sendToMatch(mid, new Response("GS_pl", sp));
    }

    public void sendToMatch(UUID mid, Response resp) {
        for (Connection con : kryoServer.getConnections()) {
            kryoServer.sendToTCP(con.getID(), resp);
        } // FIX PLEASE!!!!!
    }

    @Override
    public ServerPayload constructPayload(UUID mid) {
        UUID a_id = m.getCurrentPlayer().getPlayerId();
        UUID b_id = m.getOppoPlayer().getPlayerId();

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

        return new ServerPayload(m.getMatchId(), a_id, b_id, a_cpos, b_cpos, a_pmv, b_pmv, cballpos);
    }


    @Override
    public Response receivePayload(Object obj) {
        inBuffer.addFirst(obj); // Most recent = first
        cleanBuffers();

        return new Response(ResponseStatus.OK.name(), null);
    }
}
