package io.github.pocketrice.Prysm;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public abstract class Physicable { // determine if needed
    Vector3 pos, velo, acc;
    BoundingBox bb;
    public abstract void update(float t);
}
