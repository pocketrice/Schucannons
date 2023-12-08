package io.github.pocketrice.Prysm;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;

public class Rigidbody extends Physicable {
    ModelInstance mi;
    List<Force> forces;

    @Getter
    Vector3 location, velocity, acceleration;

    @Getter
    float mass;

//    public Rigidbody(Model model, BoundingBox bb, float m, Force... f) {
//        mi = new ModelInstance(model);
//        boundingBox = bb;
//        mass = m;
//
//        forces = new LinkedList<>();
//        forces.addAll(List.of(f));
//        location = Vector3.Zero;
//        velocity = Vector3.Zero;
//        acceleration = Vector3.Zero;
//    }

    public Rigidbody(Model model, float m, Vector3 loc, Vector3 velo, Vector3 acc, Force... f) {
        //this(model, model.calculateBoundingBox(new BoundingBox()), m, f);
        mi = new ModelInstance(model);
        bb = new BoundingBox();
        mi.calculateBoundingBox(bb);
        mass = m;

        forces = new LinkedList<>();
        forces.addAll(List.of(f));
        location = loc;
        velocity = velo;
        acceleration = acc;
    }

    public Rigidbody(Model model) {
        this(model, 10f, Vector3.Zero, Vector3.Zero, Vector3.Zero);
    }

    public void addVelocity(Vector3 vec3) {
        velocity.add(vec3);
    }

    public void addAcceleration(Vector3 vec3) {
        acceleration.add(vec3);
    }

    @Override
    public void update(float t) {
        forces.forEach(f -> f.apply(this, t)); // apply forces for given t
        velocity.add(acceleration); // update velocity, since acc is already scaled for t, no need to do so (bs solution??)
        location.add(velocity); // update loc
        mi.calculateBoundingBox(bb); // recalc bounding box (NOT efficient. Why???)
    }


    public void handleCollision(Rigidbody other) {
        if (this.bb.intersects(other.bb)) {
            moveBoundingBox(other.bb, calcSeparationVector(this.bb, other.bb));
        }
    }

    public static Vector3 calcSeparationVector(BoundingBox box1, BoundingBox box2) {
        Vector3 center1 = new Vector3();
        Vector3 center2 = new Vector3();
        box1.getCenter(center1);
        box2.getCenter(center2);

        Vector3 sepVec = new Vector3(center2).sub(center1).nor(); // gpt; get vec of c1 -> c2 and normalise

        // Scale by sum of "half-sizes" (getDim returns diagonal vec, length is taken and divided. This is an easier desc of a bounding box.)
        float halfSize1 = box1.getDimensions(new Vector3()).len() / 2f;
        float halfSize2 = box2.getDimensions(new Vector3()).len() / 2f;
        float scale = (halfSize1 + halfSize2) * 1.1f; // slight margin for safety(?)

        sepVec.scl(scale);
        return sepVec;
    }

    // Shift apart by the scaled-up separation vector
    public static void moveBoundingBox(BoundingBox box, Vector3 sepVec) {
        box.set(box.getMin(new Vector3()).add(sepVec), box.getMax(new Vector3()).add(sepVec));
    }
}
