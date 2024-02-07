package io.github.pocketrice.shared.serialisers;

import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.github.pocketrice.server.ServerPayload;

import java.time.Instant;

public class ServerPayloadSerialiser extends Serializer<ServerPayload> {
    @Override
    public void write(Kryo kryo, Output output, ServerPayload object) {
        output.writeString(object.getTimestamp().toString());
        output.writeString(object.getMatchId());
        output.writeString(object.getA_playerId());
        output.writeString(object.getB_playerId());

        Vector3 a_cp = object.getA_cannonPos();
        Vector3 b_cp = object.getB_cannonPos();
        Vector3 a_pmv = object.getA_projMotVec();
        Vector3 b_pmv = object.getB_projMotVec();
        Vector3 cbp = object.getCballPos();

        output.writeFloats(new float[]{a_cp.x, a_cp.y, a_cp.z}, 0, 3);
        output.writeFloats(new float[]{b_cp.x, b_cp.y, b_cp.z}, 0,3);
        output.writeFloats(new float[]{a_pmv.x, a_pmv.y, a_pmv.z}, 0,3);
        output.writeFloats(new float[]{b_pmv.x, b_pmv.y, b_pmv.z}, 0,3);
        output.writeFloats(new float[]{cbp.x, cbp.y, cbp.z}, 0,3);
    }

    @Override
    public ServerPayload read(Kryo kryo, Input input, Class<? extends ServerPayload> type) {
        Instant timestamp = Instant.parse(input.readString());
        String matchId = input.readString();
        String a_pid = input.readString();
        String b_pid = input.readString();
        Vector3 a_cp = new Vector3(input.readFloat(), input.readFloat(), input.readFloat());
        Vector3 b_cp = new Vector3(input.readFloat(), input.readFloat(), input.readFloat());
        Vector3 a_pmv = new Vector3(input.readFloat(), input.readFloat(), input.readFloat());
        Vector3 b_pmv = new Vector3(input.readFloat(), input.readFloat(), input.readFloat());
        Vector3 cbp = new Vector3(input.readFloat(), input.readFloat(), input.readFloat());

        return new ServerPayload(timestamp, matchId, a_pid, b_pid, a_cp, b_cp, a_pmv, b_pmv, cbp);
    }
}
