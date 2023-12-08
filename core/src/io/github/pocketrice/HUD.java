package io.github.pocketrice;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;


public class HUD {
    private SpriteBatch batch;
    private BitmapFont font;
    private Match match;

    public HUD(Match m) {
        batch = new SpriteBatch();
        font = new BitmapFont(Gdx.files.internal("fonts/sm64.fnt"));
        font.getData().setScale(0.7f);
        font.setColor(Color.valueOf("#C7C5C5"));
        match = m;
    }

    public void render() {
        Vector3 projVec = match.currentPlayer.projVector;
        batch.begin();
        font.draw(batch, "X: " + projVec.x, 30,30);
        font.draw(batch, "Y: " + projVec.y, 30,60);
        font.draw(batch, "Z: " + projVec.z, 30, 90);
        System.out.println("HUD OK");
        batch.end();
    }

    public void dispose() {
        batch.dispose();
    }
}
