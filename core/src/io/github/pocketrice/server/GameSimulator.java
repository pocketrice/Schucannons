package io.github.pocketrice.server;


import com.badlogic.gdx.math.Vector3;
import io.github.pocketrice.client.Player;
import io.github.pocketrice.server.Prysm.ForceConstant;

// all physics crap goes here. Shouldn't be coupled to a match, instead takes in locs n' crap.
public class GameSimulator {
    public Vector3 projMot(Vector3 projVec, Vector3 currLoc) {
        // ∆x = v0x*t
        // ∆y = v0y*t - 0.5gt^2
        // ∆z = v0z*t

        // for now, assume ∆y = 0.
        // 0 = v0y*t - 0.5gt^2
        // 0.5gt^2 = v0y*t
        // 0.5gt = v0y
        // t = v0y/0.5g
        float t = (float) (projVec.y / (0.5 * ForceConstant.EARTH_G.val()));
        float x = projVec.x * t;
        float z = projVec.z * t;

        return currLoc.cpy().add(x,0f,z);
    }

    public boolean isHit(Player currPlayer, Player oppoPlayer) {
        Vector3 projVec = currPlayer.getProjVector();
        Vector3 currentLoc = currPlayer.getRb().getLocation();
        Vector3 projMot = projMot(projVec, currentLoc);

        return projMot.equals(oppoPlayer.getRb().getLocation());
    }



    public boolean isLegal(Vector3 loc) {

    }

    public Vector3 legalise(Vector3 loc) {

    }
}
