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
        fontbook = Fontbook.of("tinyislanders.ttf");
        gmgr = gm;
    }

    public void render() {
        Match matchState = gmgr.getMatchState();
        Vector3 projVec = (matchState.getCurrentPlayer() == null) ? Vector3.Zero : matchState.getCurrentPlayer().getProjVector();
        batch.begin();
        fontbook.formatDraw("tinyislanders", Color.valueOf("#b8b1f2"), batch, "Match " + matchState.getIdentifier(), new Vector2(460, 820));
        fontbook.formatDraw("tinyislanders", Color.valueOf("#d0cee0"), batch, "X: " + projVec.x, new Vector2(30, 110));
        fontbook.formatDraw("tinyislanders", Color.valueOf("#d0cee0"), batch, "Y: " + projVec.y, new Vector2(30, 75));
        fontbook.formatDraw("tinyislanders", Color.valueOf("#d0cee0"), batch, "Z: " + projVec.z, new Vector2(30, 40));
        System.out.println("HUD OK");
        batch.end();
    }

    public void dispose() {
        batch.dispose();
    }
}
