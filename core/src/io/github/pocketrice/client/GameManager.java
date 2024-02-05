package io.github.pocketrice.client;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import io.github.pocketrice.server.ServerPayload;
import io.github.pocketrice.shared.Request;
import lombok.Getter;

import java.util.List;

// Does all the nitty-gritty client stuffs. Unpack server payloads, handle interp...
public class GameManager {
    private GameClient client;
    private GameRenderer grdr;

    @Getter
    private Match match; // This is a local-only compilation of ServerPayloads — for ease of storage.

    public GameManager(GameClient gc) {
        client = gc;
    }

    public void requestMatch() {
        client.kryoClient.sendTCP(new Request("GC_matches", null));
        client.kryoClient.addListener(new Listener() {
            public void received(Connection con, Object obj) { // todo: need to move this?
                if (obj instanceof List<?>) {
                   List<Match> matches = (List<Match>) obj;
                   match = matches.get(0); // todo: prompt player to pick one.
                    client.kryoClient.sendTCP(new Request("GC_selMatch", match.matchId));
                }
            }
        });
    }



    public Match decompilePayload(ServerPayload sp) { // todo: technically pos should be set by GM every frame — need to figure out.
        Player currPlayer = match.currentPlayer = match.getPlayer(sp.getA_playerId());
        Player oppoPlayer = match.oppoPlayer = match.getPlayer(sp.getB_playerId());

        currPlayer.setProjVector(sp.getA_projMotVec());
        currPlayer.rb.setLocation(sp.getA_cannonPos());
        oppoPlayer.setProjVector(sp.getB_projMotVec());
        oppoPlayer.rb.setLocation(sp.getB_cannonPos());

        match.cballPos = sp.getCballPos();

        match.timestamp = sp.getTimestamp();

        return match;
    }
}
