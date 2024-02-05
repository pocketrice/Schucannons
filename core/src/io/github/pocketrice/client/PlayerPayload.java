package io.github.pocketrice.client;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import lombok.Getter;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

// This class represents a game state, which is sent based on tickrate
@Getter
public class PlayerPayload implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    Instant timestamp;
    String playerId, matchId;
    Vector3 cannonPos;
    Vector3 projMotVec; // Normally this data should be received as an angle too, but this is packaged into a Vec3 for efficiency.
    //double cannonAngle; // Assuming spherical, phi can be ignored since it is auto-calibrated

    public PlayerPayload(String pid, String mid, Vector3 cpos, Vector2 pmv, double theta) throws IOException {
        playerId = pid;
        matchId = mid;
        cannonPos = cpos;

        // Vec2 on ZY, angle b/w XY. Convert from spherical to rectangular coords (Calc III 11.7)
        float rho = pmv.len();
        double phi = (float) Math.atan(pmv.x / pmv.y);
        projMotVec = new Vector3((float) (rho * Math.sin(phi) * Math.cos(theta)), (float) (rho * Math.sin(phi) * Math.sin(theta)), (float) (rho * Math.cos(phi))); // x = ρsinφcosθ, y = ρsinφsinθ, z = ρcosφ
        timestamp = Instant.now();
    }

    public PlayerPayload(String pid, String mid, Vector3 cpos, Vector3 pmv) {
        playerId = pid;
        matchId = mid;
        cannonPos = cpos;
        projMotVec = pmv;
        timestamp = Instant.now();
    }
}
