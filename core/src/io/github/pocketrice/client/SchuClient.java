package io.github.pocketrice.client;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import io.github.pocketrice.server.ServerPayload;
import io.github.pocketrice.shared.*;
import lombok.Setter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.UUID;


public class SchuClient extends GameClient {
    static final int AWAIT_MAX_SEC = 5;

    GameManager gmgr;
    IntervalBuffer intvBuffer;
    @Setter
    UUID matchId;
    @Setter
    boolean isMatchJoined;

    public SchuClient(GameManager gm) throws UnknownHostException {
        this(gm, 3074);
    }

    public SchuClient(GameManager gm, int port) throws UnknownHostException {
        this(gm, "", port);
        clientName =  "client-" + InetAddress.getLocalHost();
    }

    public SchuClient(GameManager gm, String name, int port) {
        clientName = name;
        startTime = Instant.now();
        tcpPort = port;
        kryoClient = new Client();
        gmgr = gm;
        self = new HumanPlayer(UUID.randomUUID(), "NOTBOT");

        inBuffer = new LinkedList<>();
        outBuffer = new LinkedList<>();
        intvBuffer = new IntervalBuffer();

        isMatchJoined = false;

        clientRate = calcClientRate();
        maxIBufferSize = maxOBufferSize = 10;

        logValid("Client " + this + " loaded @ " + startTime);
    }

    public int calcClientRate() {
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
                            case "GS_matches" -> gmgr.receiveMatchList(rp.getPayload());

                            case "GS_selMatch" -> {
                                matchId = ((ServerPayload) rp.getPayload()).getMatchId();
                                gmgr.receiveMatch(rp.getPayload());
                                gmgr.setClientConnected(true);
                            }

                            case "GS_mid" -> gmgr.receiveMatchId(rp.getPayload());

                            case "GS_plList" -> gmgr.receivePlayerList(rp.getPayload());

                            case "GS_pl" -> {
                                gmgr.receiveServerUpdate(rp.getPayload());
                            }

                            case "GS_ping" -> {
                                Instant endPingTime = Instant.now(); // Save now to ensure post-ping ops do not add delay
                                String[] payload = ((String) rp.getPayload()).split("\\|");

                                Instant serverPingTime = Instant.parse(payload[0]);
                                long pingToServer = ChronoUnit.MILLIS.between(pingTime, serverPingTime);
                                long pingToClient = ChronoUnit.MILLIS.between(serverPingTime, endPingTime);
                                ping = pingToServer + pingToClient; // combine ping halves

                                serverName = payload[1];
                                String[] splitName = payload[1].split("/");
                                serverAddress = new InetSocketAddress(splitName[splitName.length-1].split(":")[0], Integer.parseInt(splitName[splitName.length-1].split(":")[1]));

                                serverTps = Double.parseDouble(payload[2]);

                                logCon("gs_ping received. " + pingToServer + "ms (c-s), " + pingToClient + "ms (s-c), " + ping + "ms (total)");

                                if (!serverName.equals(payload[1])) {
                                    serverAddress = kryoClient.getRemoteAddressTCP();
                                    serverName = payload[1];
                                }
                            }

                            case "GS_prestart" -> {
                                log("Match prestarting!");
                                gmgr.processPrestart();
                            }

                            case "GS_phase" -> {
                                logInfo("Received phase request");
                                intvBuffer.enqueue(rp.getPayload(), ((pl) -> gmgr.receivePhaseSignal(pl)), gmgr.getBufferInterv());
                            }

                            case "GS_ackReady" -> {
                                log("Received acknowledgment of ready.");
                                gmgr.processReadyAck();
                            }

                            default -> throw new IllegalArgumentException("Invalid server response — " + rp.getMsg() + "!");
                        }
                    }
                }
            });
        });

        kryoThread.setName(this + "-KryThread");
        kryoThread.start();

        updateThread = new Thread(() -> {
            long millisSincePing = 0;

            while (true) {
                try {
                    Thread.sleep(1000 / clientRate); // NOT busywaiting! Ignore warning.
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                if (isMatchJoined) {
                    millisSincePing += 1000 / clientRate;

                    if (millisSincePing > PING_INTERVAL) {
                        ping();
                        millisSincePing = 0;
                    }

                    sendPayload();
                }

                // Request to server to fill match w/ a bot after waiting for joins
                if (gmgr.getJoinInterv().justEnded()) {
                    log(AWAIT_MAX_SEC + " sec elapsed without other player. Requesting bot player...");
                    gmgr.getJoinInterv().unstamp();
                    kryoClient.sendTCP(new Request("GC_fillMatch", gmgr.getMatchState().getMatchId().toString()));
                }

                // Request match start after start buffer
                if (gmgr.getStartInterv().justEnded()) {
                    kryoClient.sendTCP(new Request("GC_start", gmgr.getMatchState().getMatchId().toString()));
                }

                // Handle prompt end after phase timeout
                if (gmgr.getPhaseInterv().justEnded()) {
                    System.out.println("TIME OVER");
                }

                intvBuffer.update();

                Interval disconInterv = gmgr.getDisconInterv();
                if (!kryoClient.isConnected() && !disconInterv.observe()) {
                    disconInterv.stamp();
                }

                if (disconInterv.justEnded()) {
                    try {
                        reconnect();
                    } catch (IOException ignored) {}
                }
            }
        });

        updateThread.setName(this + "-UpdThread");
        updateThread.start();
    }

    @Override
    public void connect(int timeout, String ipv4, int[] ports) throws IOException {
        kryoClient.connect(timeout, ipv4, ports[0]);
        //kryoClient.sendTCP(new Request("GC_connect", null)); // needed?
    }

    @Override
    public void disconnect() {
        kryoClient.sendTCP(new Response(ResponseStatus.OK.name(), null)); // needed?
        kryoClient.stop();
    }

    @Override
    public void reconnect() throws IOException {
        kryoClient.reconnect();
        gmgr.getDisconInterv().unstamp();
    }

    @Override
    public void ping() {
        pingTime = Instant.now();
        kryoClient.sendTCP(new Request("GC_ping", null));
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
        return new PlayerPayload(self.getPlayerId(), matchId, self.getPos(), self.projVector);
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

    @Override
    public String toString() {
        return super.toString();
    }
}
