package io.github.pocketrice.client;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

// This class represents a game state, which is sent based on tickrate
@Getter
public class PlayerPayload {
    Instant timestamp;
    UUID playerId, matchId;
    Vector3 cannonPos;
    Vector3 projMotVec; // Normally this data should be received as an angle too, but this is packaged into a Vec3 for efficiency.
    //double cannonAngle; // Assuming spherical, phi can be ignored since it is auto-calibrated

    public PlayerPayload(UUID pid, UUID mid, Vector3 cpos, Vector2 pmv2, double theta) {
        playerId = pid;
        matchId = mid;
        cannonPos = cpos;

        // Vec2 on ZY, angle b/w XY. Convert from spherical to rectangular coords (Calc III 11.7)
        float rho = pmv2.len(); // does not need to be 3d b/c len is always same
        double phi = Math.atan(pmv2.y / pmv2.x);
        projMotVec = new Vector3((float) (rho * Math.sin(phi) * Math.cos(theta)), (float) (rho * Math.sin(phi) * Math.sin(theta)), (float) (rho * Math.cos(phi))); // x = ρsinφcosθ, y = ρsinφsinθ, z = ρcosφ
        timestamp = Instant.now();
    }

    public PlayerPayload(UUID pid, UUID mid, Vector3 cpos, Vector3 pmv3) {
        playerId = pid;
        matchId = mid;
        cannonPos = cpos;
        projMotVec = pmv3;
        timestamp = Instant.now();
    }

    public PlayerPayload(Instant ts, UUID pid, UUID mid, Vector3 cpos, Vector3 pmv3) {
        timestamp = ts;
        playerId = pid;
        matchId = mid;
        cannonPos = cpos;
        projMotVec = pmv3;
    }
}
