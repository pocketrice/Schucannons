package io.github.pocketrice.shared;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import lombok.Getter;
import lombok.Setter;

// Utility class for handling interp for many different datatype. DEPRECATED!
@Getter
public class Interlerper<T> {
    private T startVal, endVal;
    private EasingFunction easing;
    @Setter
    private boolean isLooping, isInterlerp;
    private boolean isForward;
    @Setter
    private double stepSize, t;



    public Interlerper(T start, T end) {
        this(start, end, EasingFunction.LINEAR, 0.2);
    }

    public Interlerper(T start, T end, EasingFunction e, double ss) {
        startVal = start;
        endVal = end;
        t = 0;
        isLooping = false;
        isInterlerp = true;
        isForward = true;
        easing = e;
        stepSize = ss;
    }


    public Interlerper<T> from(T val) {
        startVal = val;
        return this;
    }

    public Interlerper<T> to(T val) {
        endVal = val;
        return this;
    }

    public T interlerp(double t, EasingFunction easing) {
        T result = null;

        if (startVal instanceof Integer startNum) {
            int endNum = (Integer) endVal;
            result = (T) (Integer) ((Double) (startNum + easing.apply(t) * (endNum - startNum))).intValue(); // what black magic is this??
        }
        else if (startVal instanceof Float startNum) {
            float endNum = (float) endVal;
            result = (T) (Float) ((Double) (startNum + easing.apply(t) * (endNum - startNum))).floatValue();
        }
        else if (startVal instanceof Color startColor) {
            Color endColor = (Color) endVal;
            double[] rgbSc = {startColor.r, startColor.g, startColor.b, startColor.a};
            double[] rgbEc = {endColor.r, endColor.g, endColor.b, endColor.a};
            result = (T) new Color(
                    (float) (rgbSc[0] + (rgbEc[0] - rgbSc[0]) * easing.apply(t)),
                    (float) (rgbSc[1] + (rgbEc[1] - rgbSc[1]) * easing.apply(t)),
                    (float) (rgbSc[2] + (rgbEc[2] - rgbSc[2]) * easing.apply(t)),
                    (float) (rgbSc[3] + (rgbEc[3] - rgbSc[3]) * easing.apply(t)));
        }
        else if (startVal instanceof Vector2 startVec2) {
            Vector2 endVec2 = (Vector2) endVal;
            result = (T) new Vector2(startVec2.x + (float) easing.apply(t) * (endVec2.x - startVec2.x), startVec2.y + (float) easing.apply(t) * (endVec2.y - startVec2.y));
        }
        else if (startVal instanceof Vector3 startVec3) {
            Vector3 endVec3 = (Vector3) endVal;
            result = (T) new Vector3(startVec3.x + (float) easing.apply(t) * (endVec3.x - startVec3.x), startVec3.y + (float) easing.apply(t) * (endVec3.y - startVec3.y), startVec3.z + (float) easing.apply(t) * (endVec3.z - startVec3.z));
        }
        else if (startVal instanceof Interlerpable startLerp) {
            result = (T) startLerp.interlerp(t);
        }

        return result;
    }

    public void setForward(boolean isForward) {
        this.isForward = isForward;
        isInterlerp = true; // Enable interlerping (again), since either disabled after terminal point or still running.
    }

    public double advanceParam() {
        if (isLooping) {
            t = (isForward) ? (t + stepSize) % 1f : (t - stepSize) % 1f;
        } else {
            t = (isForward) ? Math.min(1.0, t + stepSize) : Math.max(0, t - stepSize); // EVIL BUG!!!!11!!! The fabled "min-max flip flop" bug. Total of 2 hours spent debugging. Cheeeeers! :DD
        }
        return t;
    }

    // Value-only interlerp; useful for lighter uses of interlerp.
    public T advance() {
        advanceParam();
        return interlerp(t, easing);
    }
}
