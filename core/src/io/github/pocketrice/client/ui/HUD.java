package io.github.pocketrice.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import io.github.pocketrice.client.Fontbook;
import io.github.pocketrice.client.GameManager;
import io.github.pocketrice.client.Match;


public class HUD {
    private final Fontbook fontbook;
    private GameManager gmgr;
    private SpriteBatch batch;



    public HUD(GameManager gm) {
        batch = new SpriteBatch();
        fontbook = Fontbook.of("tinyislanders.ttf", "koholint.ttf", "dina.ttc", "tf2build.ttf", "tf2segundo.ttf", "delfino.ttf", "kyomadoka.ttf");
        gmgr = gm;
    }

    public void render() {
        Match matchState = gmgr.getMatchState();
        Vector3 projVec = (matchState.getCurrentPlayer() == null) ? Vector3.Zero : matchState.getCurrentPlayer().getProjVector();
        batch.begin();
        fontbook.formatDraw("kyomadoka", 30, Color.valueOf("#b8b1f2AF"), batch, "Match " + matchState.getIdentifier(), new Vector2(560, 800), 300f);
        fontbook.formatDraw("tinyislanders", 27, Color.valueOf("#d0cee0AF"), batch, "X: " + projVec.x, new Vector2(30, 110), 70f);
        fontbook.formatDraw("tinyislanders", 27, Color.valueOf("#d0cee0AF"), batch, "Y: " + projVec.y, new Vector2(30, 85), 70f);
        fontbook.formatDraw("tinyislanders", 27, Color.valueOf("#d0cee0AF"), batch, "Z: " + projVec.z, new Vector2(30, 60), 70f);
        batch.end();
    }

    public void dispose() {
        batch.dispose();
    }
}
