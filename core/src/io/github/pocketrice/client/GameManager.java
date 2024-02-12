package io.github.pocketrice.client;

import com.badlogic.gdx.math.Vector3;
import io.github.pocketrice.server.ServerPayload;
import io.github.pocketrice.shared.Request;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

// Does all the nitty-gritty client stuffs. Unpack server payloads, handle interp...
public class GameManager {
    @Getter @Setter
    private GameClient client;
    @Setter
    private GameRenderer grdr;
    private Match match; // This is a local-only compilation of ServerPayloads — for ease of storage.
    @Getter
    private String[] matchlist; // temp
    @Getter @Setter
    private boolean isClientConnected;

    public GameManager() {
        match = new Match();
        isClientConnected = false;
    }
    public void requestMatchlist() {
        client.kryoClient.sendTCP(new Request("GC_matches", null));
    }

    public void receiveMatch(Object payload) {
        match = decompilePayload((ServerPayload) payload);
        ((SchuClient) client).setMatchStarted(true);
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
        client.kryoClient.sendTCP(new Request("GC_selMatch", new Object[]{mid.toString(), ((SchuClient) client).self}));
    }

    public void receiveServerUpdate(Object payload) {
        decompilePayload((ServerPayload) payload);
        grdr.update();
    }

    public void receivePlayerList(Object payload) {
        String[] playerList = ((String) payload).split("&");

        for (String playerEntry : playerList) {
            String[] playerData = playerEntry.split("\\|");
            UUID pid = UUID.fromString(playerData[0]);

            if (pid.equals(client.getSelf().playerId)) {
                match.addPlayers(client.getSelf());
            } else {
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

    public Vector3[] retrievePlayerState(UUID pid) {
        Vector3[] pstate = new Vector3[2];
        Player player = match.getPlayer(pid);

        pstate[0] = player.pos;
        pstate[1] = player.projVector;

        return pstate;
    }
}
