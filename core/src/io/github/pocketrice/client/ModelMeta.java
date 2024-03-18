package io.github.pocketrice.client;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import io.github.pocketrice.shared.Interlerper;

public record ModelMeta(Interlerper<Vector3> interpPos, Interlerper<Quaternion> interpRot, Vector3 posOffset, Quaternion rotOffset, float scl) {
    public ModelMeta() {
        this(new Interlerper<>(new Vector3(), new Vector3()), new Interlerper<>(new Quaternion(), new Quaternion()), new Vector3(), new Quaternion(), 1f);
    }

    public ModelMeta(Vector3 pOff, Quaternion rOff) {
        this(new Interlerper<>(new Vector3(), new Vector3()), new Interlerper<>(new Quaternion(), new Quaternion()), pOff, rOff, 1f);
    }
}
