package io.github.pocketrice.shared;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryo.Kryo;
import de.javakaffee.kryoserializers.UUIDSerializer;
import io.github.pocketrice.client.HumanPlayer;
import io.github.pocketrice.client.PlayerPayload;
import io.github.pocketrice.client.PlayerTurnPayload;
import io.github.pocketrice.server.ServerPayload;
import io.github.pocketrice.shared.serialisers.PlayerPayloadSerialiser;
import io.github.pocketrice.shared.serialisers.PlayerTurnPayloadSerialiser;
import io.github.pocketrice.shared.serialisers.ServerPayloadSerialiser;

import java.time.Instant;
import java.util.UUID;

public class KryoInitialiser {
    public static void registerClasses(Kryo kryo) { // hard-coded
        kryo.register(Request.class);
        kryo.register(Response.class);
        kryo.register(ResponseStatus.class);
        kryo.register(Vector3.class);
        kryo.register(Vector2.class);
        kryo.register(UUID.class, new UUIDSerializer()); // Classes w/ arg-only constructors need a custom Kryo Serialiser
        kryo.register(Instant.class);
        kryo.register(ServerPayload.class, new ServerPayloadSerialiser());
        kryo.register(PlayerPayload.class, new PlayerPayloadSerialiser());
        kryo.register(PlayerTurnPayload.class, new PlayerTurnPayloadSerialiser());
        kryo.register(Object[].class);
        kryo.register(HumanPlayer.class);
    }
}
