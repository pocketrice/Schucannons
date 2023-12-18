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
        match = m;
    }

    public void render() {
        //Gdx.app.postRunnable(() -> {
            //System.out.println("HUD RENDER");
            Vector3 projVec = (match.currentPlayer == null) ? Vector3.Zero : match.currentPlayer.projVector;
            batch.begin();
            formatDraw(batch, "Match " + match.getIdentifier(), 460, 820, font, Color.valueOf("#b8b1f2"));
            formatDraw(batch, "X: " + projVec.x, 30, 110, font, Color.valueOf("#d0cee0"));
            formatDraw(batch, "Y: " + projVec.y, 30, 75, font, Color.valueOf("#d0cee0"));
            formatDraw(batch, "Z: " + projVec.z, 30, 40, font, Color.valueOf("#d0cee0"));
            //System.out.println("HUD OK");
            batch.end();
        //});
    }

    public static void formatDraw(SpriteBatch batch, String str, float x, float y, BitmapFont bmf, Color color) {
        Color oldCol = bmf.getColor();
        bmf.setColor(color);
        bmf.draw(batch, str, x, y);
        bmf.setColor(oldCol); // Set color back to default
    }

    public void dispose() {
        batch.dispose();
    }
}
