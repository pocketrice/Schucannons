package io.github.pocketrice.client;

import io.github.pocketrice.server.ServerPayload;
import io.github.pocketrice.shared.Request;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
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

    public GameManager() {
        match = new Match();
    }
    public void requestMatchlist() {
        client.kryoClient.sendTCP(new Request("GC_matches", null));
    }

    public void receiveMatch(Object payload) {
        match = decompilePayload((ServerPayload) payload);
    }

    public void receiveMatchId(Object payload) {
        String[] ids = ((String) payload).split("&");
        System.out.println(Arrays.toString(ids));
        match.matchId = UUID.fromString(ids[0]);
        match.matchName = (ids[1].equals("null")) ? "" : ids[1];
    }

    public void receiveMatchList(Object payload) {
        matchlist = ((String) payload).split("&");
    }

    public void sendSelMatch(String mid) {
        client.kryoClient.sendTCP(new Request("GC_selMatch", new Object[]{mid, ((SchuClient) client).self}));
    }

    public void receiveServerUpdate(Object payload) {
        decompilePayload((ServerPayload) payload);
        grdr.update();
    }

    public Match decompilePayload(ServerPayload sp) { // todo: technically pos should be set by GM every frame — need to figure out.
        Player currPlayer = match.currentPlayer = match.getPlayer(sp.getA_playerId());
        Player oppoPlayer = match.oppoPlayer = match.getPlayer(sp.getB_playerId());

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
}
