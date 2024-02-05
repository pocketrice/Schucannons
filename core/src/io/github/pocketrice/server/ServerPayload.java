package io.github.pocketrice.server;

import com.badlogic.gdx.math.Vector3;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

// This class represents a total match state, which is sent based on tickrate
@Getter
public class ServerPayload implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    Instant timestamp;
    String matchId, a_playerId, b_playerId;

    // Avoid just using Player objects; there is redundant info that need not be transmitted.
    Vector3 a_cannonPos, b_cannonPos, a_projMotVec, b_projMotVec; // Cannon can be oriented in direction of pmv, as that was based on that angle anyway.
    Vector3 cballPos; // Rather than spawning and destroying the cannonball, instead one single cannonball is persistent and teleported into the cannon prior to firing. Upon firing, the cball is "visible" and upon finishing after certain period it is teleported back. This avoids an "optional" server payload param too.

    public ServerPayload(String mid, String apid, String bpid, Vector3 acp, Vector3 bcp, Vector3 apmv, Vector3 bpmv, Vector3 cbp) {
        matchId = mid;
        a_playerId = apid;
        b_playerId = bpid;
        a_cannonPos = acp;
        b_cannonPos = bcp;
        a_projMotVec = apmv;
        b_projMotVec = bpmv;
        cballPos = cbp;
        timestamp = Instant.now();
    }
}