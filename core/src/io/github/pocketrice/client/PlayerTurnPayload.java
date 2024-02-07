package io.github.pocketrice.client;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import lombok.Getter;

import java.time.Instant;

// This class represents a player state, which is sent based on tickrate. It does not extend PlayerPayload as this is separate and as a result doesn't replace normal payload.
@Getter
public class PlayerTurnPayload {
    Instant timestamp;
    Vector3 finalPmv; // this might be unnecessary. Maybe instead marks a previous packet as the correct vec?

    public PlayerTurnPayload(Vector2 pmv2, double theta) {
        // Vec2 on ZY, angle b/w XY. Convert from spherical to rectangular coords (Calc III 11.7)
        float rho = pmv2.len();
        double phi = (float) Math.atan(pmv2.x / pmv2.y);
        finalPmv = new Vector3((float) (rho * Math.sin(phi) * Math.cos(theta)), (float) (rho * Math.sin(phi) * Math.sin(theta)), (float) (rho * Math.cos(phi))); // x = ρsinφcosθ, y = ρsinφsinθ, z = ρcosφ
        timestamp = Instant.now();
    }

    public PlayerTurnPayload(Vector3 pmv3) {
        finalPmv = pmv3;
        timestamp = Instant.now();
    }

    public PlayerTurnPayload(Instant ts, Vector3 pmv3) {
        timestamp = ts;
        finalPmv = pmv3;
    }

}
