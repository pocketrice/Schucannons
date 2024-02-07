package io.github.pocketrice.shared.serialisers;

import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.github.pocketrice.client.PlayerTurnPayload;

import java.time.Instant;

public class PlayerTurnPayloadSerialiser extends Serializer<PlayerTurnPayload> {
    @Override
    public void write(Kryo kryo, Output output, PlayerTurnPayload object) {
        output.writeString(object.getTimestamp().toString());

        Vector3 fpmv = object.getFinalPmv();
        output.writeFloats(new float[]{fpmv.x, fpmv.y, fpmv.z}, 0, 3);
    }

    @Override
    public PlayerTurnPayload read(Kryo kryo, Input input, Class<? extends PlayerTurnPayload> type) {
        Instant timestamp = Instant.parse(input.readString());
        Vector3 fpmv = new Vector3(input.readFloat(), input.readFloat(), input.readFloat());

        return new PlayerTurnPayload(timestamp, fpmv);
    }
}
