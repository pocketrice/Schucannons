package io.github.pocketrice.client;

import com.badlogic.gdx.math.Vector3;
import io.github.pocketrice.client.Match.PhaseType;
import io.github.pocketrice.server.ServerPayload;
import io.github.pocketrice.shared.Request;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

// Does all the nitty-gritty client stuffs. Unpack server payloads, handle interp...
public class GameManager {
    public static final int START_MAX_DELAY = 5;

    private Audiobox audiobox;
    private Fontbook fontbook;
    @Getter @Setter
    private GameClient client;
    @Getter
    private SchuAssetManager amgr;
    @Setter
    private GameRenderer grdr;
    @Getter
    private SchuGame game;
    private Match match; // This is a local-only compilation of ServerPayloads — for ease of storage.
    @Getter
    private String[] matchlist; // temp


    @Getter @Setter
    private boolean isClientConnected, isRunningPhase;
    @Getter @Setter
    private int phaseDuration, phaseDelay;
    @Getter @Setter
    private Instant phaseStartInstant, joinInstant, startInstant;
    @Getter
    private float movePhaseMaxDist;

    public GameManager(SchuGame sg) {
        match = new Match();
        game = sg;
        amgr = game.getAmgr();
        audiobox = amgr.getAudiobox();
        fontbook = amgr.getFontbook();
        fontbook.setAmgr(amgr);
        audiobox.setAmgr(amgr);

        isClientConnected = false;
    }
    public void requestMatchlist() {
        client.kryoClient.sendTCP(new Request("GC_matches", null));
    }

    public void receiveMatch(Object payload) {
        match = decompilePayload((ServerPayload) payload);
        ((SchuClient) client).setMatchJoined(true);
    }

    public void receiveMatchId(Object payload) {
        String[] ids = ((String) payload).split("\\|");
        match.matchId = UUID.fromString(ids[0]);
        match.matchName = (ids[1].equals("null")) ? "" : ids[1];
    }

    public void receiveMatchList(Object payload) {
        matchlist = ((String) payload).split("&");
    }

    public void sendSelMatch(UUID mid) {
        client.kryoClient.sendTCP(new Request("GC_selMatch", new Object[]{mid.toString(), client.self}));
    }

    public void receiveServerUpdate(Object payload) {
        decompilePayload((ServerPayload) payload);

        // Wait for assets/GameRenderer to be completely loaded. Notice that client/server communication is done prior to waiting.
        if (grdr != null) {
            grdr.update();
        }
    }

    public void receivePlayerList(Object payload) {
        boolean hasPlayedSfx = false;
        String[] playerList = ((String) payload).split("&");

        for (String playerEntry : playerList) {
            String[] playerData = playerEntry.split("\\|");
            UUID pid = UUID.fromString(playerData[0]);

            if (!match.hasPlayer(pid)) {
                if (pid.equals(client.getSelf().playerId)) {
                    match.addPlayers(client.getSelf());
                } else {
                    if (!hasPlayedSfx) {
                        audiobox.playSfx("notification_alert", 0.4f);
                        hasPlayedSfx = true;
                    }

                    String pname = playerData[1];
                    String ptype = playerData[2];
                    Player pl;

                    switch (ptype) {
                        case "HumanPlayer" -> pl = new HumanPlayer(pid, pname);

                        case "BotPlayer" -> pl = new BotPlayer(pid, pname, Integer.parseInt(playerData[3]), Boolean.parseBoolean(playerData[4]));

                        default -> pl = new SpectatorPlayer(new HumanPlayer(pid, pname));
                    }

                    match.addPlayers(pl);
                }
            }
        }

        Arrays.stream(match.players()).filter(p -> !match.hasPlayer(p.getPlayerId())).forEach(p -> match.kickPlayer(p)); // Kick all outdated players
    }

    public void receivePhaseSignal(Object payload) {
        String[] phaseInfo = ((String) payload).split("\\|"); // [ phaseTime, movePhaseMaxDist (opt) ]
        phaseDuration = Integer.parseInt(phaseInfo[0]);
        phaseStartInstant = Instant.now();
        match.advancePhase();

        PhaseType phase = match.getPhase();

        switch (phase) {
            case MOVE -> {
                movePhaseMaxDist = Float.parseFloat(phaseInfo[1]);
                client.log("Move phase started!");
            }
            case PROMPT -> {
                audiobox.playSfx("dominate", 0.5f);
                client.log("Prompt phase started!");
            }

            case SIM -> {
                audiobox.playSfx("revenge", 0.5f);
                client.log("Sim phase started!");
            }

            case ENDED -> {
                client.log("Turn ended.");
            }
        }
    }

    public void processReadyAck() {
        joinInstant = Instant.now();
    }

    public Match decompilePayload(ServerPayload sp) { // todo: technically pos should be set by GM every frame — need to figure out.
        Player currPlayer = (sp.getA_playerId() == null) ? null : match.getPlayer(sp.getA_playerId());
        Player oppoPlayer = (sp.getB_playerId() == null) ? null : match.getPlayer(sp.getB_playerId());

        if (currPlayer != null) {
            currPlayer.setProjVector(sp.getA_projMotVec());
            currPlayer.setPos(sp.getA_cannonPos());
        }
        if (oppoPlayer != null) {
            oppoPlayer.setProjVector(sp.getB_projMotVec());
            oppoPlayer.setPos(sp.getB_cannonPos());
        }

        match.cballPos = sp.getCballPos();

        match.timestamp = sp.getTimestamp();

        return match;
    }

    public Match getMatchState() {
        return match;
    }

    public void processPrestart() {
        if (match.getState() != Match.GameState.READY) client.logErr("Server dictated ready but game state out-of-sync.");
        startInstant = Instant.now();
        audiobox.playSfx("aero-seatbelt", 3f);
    }

    public void requestStart() {
        client.kryoClient.sendTCP(new Request("GC_start", match.getMatchId().toString()));
    }

    public Vector3[] retrievePlayerState(UUID pid) {
        Vector3[] pstate = new Vector3[2];
        Player player = match.getPlayer(pid);

        pstate[0] = player.pos;
        pstate[1] = player.projVector;

        return pstate;
    }

    public void submitPhase() {
        isRunningPhase = false;
        client.kryoClient.sendTCP(new Request("GC_submitPhase", null));
    }
}
