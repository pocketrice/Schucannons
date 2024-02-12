package io.github.pocketrice.shared.serialisers;

import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.github.pocketrice.client.PlayerPayload;

import java.time.Instant;
import java.util.UUID;

public class PlayerPayloadSerialiser extends Serializer<PlayerPayload> {
    @Override
    public void write(Kryo kryo, Output output, PlayerPayload object) {
        output.writeString(object.getTimestamp().toString());
        output.writeString(object.getPlayerId().toString());
        output.writeString(object.getMatchId().toString());

        Vector3 cpos = object.getCannonPos();
        Vector3 pmv = object.getProjMotVec();

        output.writeFloats(new float[]{cpos.x, cpos.y, cpos.z}, 0, 3);
        output.writeFloats(new float[]{pmv.x, pmv.y, pmv.z}, 0, 3);
    }

    @Override
    public PlayerPayload read(Kryo kryo, Input input, Class<? extends PlayerPayload> type) {
        Instant timestamp = Instant.parse(input.readString());
        UUID playerId = UUID.fromString(input.readString());
        UUID matchId = UUID.fromString(input.readString());

        Vector3 cpos = new Vector3(input.readFloat(), input.readFloat(), input.readFloat());
        Vector3 pmv = new Vector3(input.readFloat(), input.readFloat(), input.readFloat());

        return new PlayerPayload(timestamp, playerId, matchId, cpos, pmv);
    }
}
