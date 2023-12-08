package io.github.pocketrice.Prysm;

import com.badlogic.gdx.math.Vector3;

public class Force {
    static final float EARTH_G = 9.8f;

    Vector3 forceVec;


    public Force(Vector3 vec) {
        forceVec = vec;
    }


    // F = ma -> a = F/m
    public void apply(Rigidbody rb, float t) {
        rb.addAcceleration(forceVec.scl(t / rb.getMass())); // a = F * (1/m * t)
    }

    public static Force gravForce(float g) {
        return new Force(new Vector3(0f, -g, 0f));
    }

    public Vector3 vec() {
        return forceVec;
    }
}
