package io.github.pocketrice;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector3;
import io.github.pocketrice.Prysm.Rigidbody;
import lombok.Getter;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.DoubleStream;

@Getter
public class BotPlayer extends Player {
    int difficulty;
    @Getter
    boolean isDummy;




    public static final BotPlayer FILLER = new BotPlayer(0, false);

    public BotPlayer() {
        this(0, true);
        Model model = new GLTFLoader().load(Gdx.files.internal("models/schucannon.gltf")).scene.model;
        health = 3;
        rb = new Rigidbody(model);
        playerId = UUID.randomUUID();
    }

    public BotPlayer(int d, boolean dummy) {
        difficulty = d;
        isDummy = dummy;
        Model model = new GLTFLoader().load(Gdx.files.internal("models/schucannon.gltf")).scene.model;
        health = 3;
        rb = new Rigidbody(model);
        playerId = UUID.randomUUID();
    }

    public void reset() {

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

    @Override
    public void deductHealth() {
        health--;
    }

    @Override
    public void requestProjVector() {
        // FIXME please :)
        projVector = new Vector3(proximityRandom(3f, 1f,1f), proximityRandom(3f, 1f,1f), proximityRandom(3f, 1f,1f));
    }

    @Override
    public String toString() {
        return "Bot player " + (playerName.isEmpty() ? playerId : playerName);
    }
}
