package io.github.pocketrice.client;

import com.badlogic.gdx.math.Vector3;
import io.github.pocketrice.client.Match.PhaseType;
import io.github.pocketrice.server.ServerPayload;
import io.github.pocketrice.shared.Request;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

// Does all the nitty-gritty client stuffs. Unpack server payloads, handle interp...
public class GameManager {
    public static final int MATCH_START_MAX_DELAY_SEC = 10;

    static final Audiobox audiobox = Audiobox.of(List.of("dominate.ogg", "revenge.ogg", "aero-seatbelt.ogg", "notification_alert.ogg"), List.of());

    @Getter @Setter
    private GameClient client;
    @Setter
    private GameRenderer grdr;
    @Getter
    private SchuGame game;
    private Match match; // This is a local-only compilation of ServerPayloads — for ease of storage.
    @Getter
    private String[] matchlist; // temp
    @Getter
    private PhaseType phaseType;


    @Getter @Setter
    private boolean isClientConnected;
    @Getter @Setter
    private int phaseTime;
    @Getter @Setter
    private Instant phaseStartInstant, joinInstant, startInstant;
    @Getter
    private float movePhaseMaxDist;

    public GameManager(SchuGame sg) {
        match = new Match();
        game = sg;
        isClientConnected = false;
        phaseType = PhaseType.INVALID;
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
        grdr.update();
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
                        audiobox.playSfx("notification_alert", 0.8f);
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

    public void receivePhaseSignal(Object payload, PhaseType phase) {
        String[] phaseInfo = ((String) payload).split("\\|"); // [ phaseTime, movePhaseMaxDist (opt) ]
        phaseTime = Integer.parseInt(phaseInfo[0]);
        phaseType = phase;
        phaseStartInstant = Instant.now();

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
        if (match.getGameState() != Match.GameState.READY) client.logErr("Server dictated ready but game state out-of-sync.");
        startInstant = Instant.now();
        audiobox.playSfx("aero-seatbelt", 1f);
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

}
