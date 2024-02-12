package io.github.pocketrice.server;

import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import io.github.pocketrice.client.*;
import io.github.pocketrice.shared.KryoInitialiser;
import io.github.pocketrice.shared.Request;
import io.github.pocketrice.shared.Response;
import io.github.pocketrice.shared.ResponseStatus;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.pocketrice.shared.AnsiCode.ANSI_BLUE;
import static io.github.pocketrice.shared.AnsiCode.ANSI_RESET;

public class DedicatedServer extends GameServer {
    public static final int TICKS_PER_CHECK = 20;
    Matchmaker mm;
    GameSimulator gsim;
    Map<UUID, Set<Connection>> clientMap;



    public DedicatedServer() throws UnknownHostException {
        this(3074);
    }

    public DedicatedServer(int port) throws UnknownHostException {
        this("", port); // Note: cannot manually use server.tostring() ever b/c though it is set, it somehow becomes an anonymous inner class I think? (io.github.pocketrice.server.DedicatedServer$1@38bc504f; notice $1) in start(). So, always use serverName. Guaranteed to be set to smth significant anyway.
        serverName = this.getClass().getSimpleName() + "-" + InetAddress.getLocalHost() + ":" + tcpPort;
    }

    public DedicatedServer(String name, int port) {
        serverName = name;
        tcpPort = port;
        kryoServer = new Server();

        mm = new Matchmaker();
        gsim = new GameSimulator();
        clients = new ArrayList<>();
        clientMap = new TreeMap<>();
        inBuffer = new LinkedList<>();
        outBuffer = new LinkedList<>();

        tickRate = 20;
        tickCounter = 0;
        tps = 0.0;
        maxClients = 20;
        maxIBufferSize = maxOBufferSize = 10;
    }

    // This should be done (opt'l) prior to closing a server.
    public void transfer(GameServer server) {
        System.out.println("Clients moved from " + this.serverName + " to " + server.serverName);
        for (GameClient cl : clients) {
            server.connectClient(cl);
            this.disconnectClient(cl);
        }
    }

    public PlayerPayload findMostRecentPayload(UUID pid) {
        PlayerPayload pp = null;
        for (int i = 0; pp == null && i < inBuffer.size(); i++) {
            if (((PlayerPayload) inBuffer.
                    get(i)).getPlayerId().equals(pid)) {
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
                public void received(Connection con, Object obj) {
                    if (obj instanceof Request rq) {
                        switch (rq.getMsg()) {
                            case "GC_matches" -> {
                                System.out.println("GC_MATCHES");
                                kryoServer.sendToTCP(con.getID(), new Response("GS_matches", mm.availableMatches.stream().map(Match::toString).collect(Collectors.joining("&"))));
                            }

                            case "GC_selMatch" -> {
                                System.out.println("GC_SELMATCH");
                                Object[] payload = (Object[]) rq.getPayload();
                                UUID mid = UUID.fromString((String) payload[0]);
                                Player player = (Player) payload[1];

                                Match m = mm.findMatch(mid.toString());
                                m.addPlayers(player);
                                clientMap.putIfAbsent(mid, new HashSet<>());
                                clientMap.get(mid).add(con);

                                kryoServer.sendToTCP(con.getID(), new Response("GS_selMatch", constructPayload(mid)));
                                kryoServer.sendToTCP(con.getID(), new Response("GS_plList", Arrays.stream(m.players()).map(p -> p.getPlayerId() + "|" + p.getPlayerName()  + "|" + p.getClass().getSimpleName() + ((p instanceof BotPlayer) ? "|" + ((BotPlayer) p).getDifficulty() + "|" + ((BotPlayer) p).isDummy() : "")).collect(Collectors.joining("&"))));
                                kryoServer.sendToTCP(con.getID(), new Response("GS_mid", m.getMatchId() + "|" + m.getMatchName()));
                            }

                            case "GC_pp", "GC_ptp" -> // todo: correct?
                                    receivePayload(rq.getPayload());

                            case "GC_ping" -> {
                                System.out.println(ANSI_BLUE + "[GS] gc_ping received." + ANSI_RESET);
                                kryoServer.sendToTCP(con.getID(), new Response("GS_ping", Instant.now() + "|" + serverName + "|" + tps));
                            }

                            default -> throw new IllegalArgumentException("Invalid client request — " + rq.getMsg() + "!");
                        }
                    }
                }
            });
        });

        kryoThread.setName(this + "-KryThread");
        kryoThread.start();

        updateThread = new Thread(() -> { // Updates every tick (66 Hz).
            Instant tickCheckStart = Instant.now();

            while (true) {
                tickCounter++;

                /*
                 * TPS calculator
                 */
                if (tickCounter >= TICKS_PER_CHECK) {
                    tps = (double) tickCounter / ChronoUnit.SECONDS.between(tickCheckStart, Instant.now());
                    tickCounter = 0;
                    tickCheckStart = Instant.now();
                }

                try {
                    Thread.sleep(1000 / tickRate); // NOT busywaiting! Ignore warning.
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                Set<Match> conMatches = mm.matches.stream().filter(m -> clientMap.containsKey(m.getMatchId())).collect(Collectors.toSet());
                for (Match m : conMatches) {
                    sendPayload(m.getMatchId());
                }
            }
        });
        updateThread.setName(this + "-UpdThread");
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
        //System.out.println("GS_PL -> " + mid);
        sendToMatch(mid, new Response("GS_pl", sp));
    }

    public void sendToMatch(UUID mid, Response resp) {
        for (Connection con : clientMap.get(mid)) {
            kryoServer.sendToTCP(con.getID(), resp);
        }
    }

    @Override
    public ServerPayload constructPayload(UUID mid) {
        Match m = mm.findMatch(mid.toString());
        UUID a_id = (m.getCurrentPlayer() == null) ? null : m.getCurrentPlayer().getPlayerId();
        UUID b_id = (m.getOppoPlayer() == null) ? null : m.getOppoPlayer().getPlayerId();

        PlayerPayload a_pp = findMostRecentPayload(a_id);
        PlayerPayload b_pp = findMostRecentPayload(b_id);

        Vector3 a_cpos = (a_pp == null) ? Vector3.Zero : a_pp.getCannonPos(); // todo: interp?
        Vector3 b_cpos = (b_pp == null) ? Vector3.Zero : b_pp.getCannonPos();

        Vector3 a_pmv = (a_pp == null) ? Vector3.Zero : a_pp.getProjMotVec();
        Vector3 b_pmv = (b_pp == null) ? Vector3.Zero : b_pp.getProjMotVec();

        // ∆x = v0x*t
        // ∆y = v0y*t - 0.5gt^2
        // ∆z = v0z*t


        float t = (a_pp == null) ? 0f : Instant.from(a_pp.getTimestamp()).toEpochMilli();

        Vector3 cballpos = (a_pp == null) ? Vector3.Zero : a_pp.getCannonPos().set(gsim.projMot(a_pmv, a_cpos, t)); // todo: validate if this is right.

        return new ServerPayload(mid, a_id, b_id, a_cpos, b_cpos, a_pmv, b_pmv, cballpos);
    }



    @Override
    public Response receivePayload(Object obj) {
        inBuffer.addFirst(obj); // Most recent = first
        cleanBuffers();

        return new Response(ResponseStatus.OK.name(), null);
    }
}
