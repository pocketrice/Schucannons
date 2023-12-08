package io.github.pocketrice;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import lombok.Getter;

import java.util.Arrays;
import java.util.stream.DoubleStream;

@Getter
public class BotPlayer extends Player {
    int difficulty;
    @Getter
    boolean isDummy;



    public static final BotPlayer FILLER = new BotPlayer(0, true);

    public BotPlayer() {
        this(0, true);
    }

    public BotPlayer(int d, boolean dummy) {
        difficulty = d;
        isDummy = dummy;
    }

    public void reset() {

    }

    @Override
    public void deductPoint() {

    }

    @Override
    public Vector3 requestProjVector() {
        // FIXME please :)
        return new Vector3(proximityRandom(3f, 1f,1f), proximityRandom(3f, 1f,1f), proximityRandom(3f, 1f,1f));
    }

    @Override
    public String toString() {
        return null;
    }

    public static <T> T weightedRandom(T[] choices, double[] weights, boolean autoEqualize)
    {
        double rng = Math.random();

        if (autoEqualize) {
            Arrays.fill(weights, 1.0 / choices.length);
        }

        assert (DoubleStream.of(weights).sum() != 1) : "Error: weightedRandom weights do not add up to 1 (= " + DoubleStream.of(weights).sum() + ")!";
        assert (choices.length == weights.length) : "Error: weightedRandom choice (" + choices.length + ") and weights (" + weights.length + ") array are not the same length!";

        for (int i = 0; i < weights.length; i++) {
            if (rng < weights[i])
                return choices[i];
            else
                rng -= weights[i];
        }

        return choices[choices.length-1];
    }

    public static float proximityRandom(float base, float lowerOffset, float upperOffset) { // <+> APM
        return (float) (Math.random() * (lowerOffset + upperOffset) + base - lowerOffset);
    }

    public Color generateRandomColor() {
        return new Color((float)(Math.random() * 255), (float)(Math.random() * 255), (float)(Math.random() * 255), 1f);
    }
}
