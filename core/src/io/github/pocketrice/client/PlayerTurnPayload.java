package io.github.pocketrice.client;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

// This class represents a player state, which is sent based on tickrate. It does not extend PlayerPayload as this is separate and as a result doesn't replace normal payload.
public class PlayerTurnPayload implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    Instant timestamp;
    Vector3 finalPmv; // this might be unnecessary. Maybe instead marks a previous packet as the correct vec?

    public PlayerTurnPayload(Vector2 pmv, double theta) {
        // Vec2 on ZY, angle b/w XY. Convert from spherical to rectangular coords (Calc III 11.7)
        float rho = pmv.len();
        double phi = (float) Math.atan(pmv.x / pmv.y);
        finalPmv = new Vector3((float) (rho * Math.sin(phi) * Math.cos(theta)), (float) (rho * Math.sin(phi) * Math.sin(theta)), (float) (rho * Math.cos(phi))); // x = ρsinφcosθ, y = ρsinφsinθ, z = ρcosφ
        timestamp = Instant.now();
    }

    public PlayerTurnPayload(Vector3 pmv) {
        finalPmv = pmv;
        timestamp = Instant.now();
    }

}
