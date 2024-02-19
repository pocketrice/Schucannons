package io.github.pocketrice.server;


import com.badlogic.gdx.math.Vector3;
import io.github.pocketrice.client.Player;
import io.github.pocketrice.server.Prysm.ForceConstant;

// all physics crap goes here. Shouldn't be coupled to a match, instead takes in locs n' crap. It also manages turn/match logic,
// broadcasting to clients TurnState and GameState.
public class GameSimulator {
    static final int MOVE_PHASE_SEC = 30, PROMPT_PHASE_SEC = 120, SIM_PHASE_SEC = 60;
    static final float MOVE_PHASE_MAX_DIST = 10f;

    public Vector3 projMot(Vector3 projVec, Vector3 currLoc, float t) {
        // ∆x = v0x*t
        // ∆y = v0y*t - 0.5gt^2
        // ∆z = v0z*t

        float x = projVec.x * t;
        float y = projVec.y * t - 0.5f * ForceConstant.EARTH_G.val() * t * t;
        float z = projVec.z * t;

        return currLoc.add(x,y,z);
    }

    public boolean isHit(Player currPlayer, Player oppoPlayer) {
        Vector3 projVec = currPlayer.getProjVector();
       // Vector3 currentLoc = currPlayer.getRb().getLocation();
       // Vector3 projMot = projMot(projVec, currentLoc);
return true;
       // return projMot.equals(oppoPlayer.getRb().getLocation());
    }
}
